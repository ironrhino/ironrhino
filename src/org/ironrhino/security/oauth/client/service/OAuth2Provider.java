package org.ironrhino.security.oauth.client.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.HttpClientUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.client.model.OAuth2Token;
import org.ironrhino.security.oauth.client.model.OAuthToken;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.util.OAuthTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public abstract class OAuth2Provider extends AbstractOAuthProvider {

	protected static Logger logger = LoggerFactory
			.getLogger(OAuth2Provider.class);

	public abstract String getAuthorizeUrl();

	public abstract String getAccessTokenEndpoint();

	@Override
	public String getVersion() {
		return "v2";
	}

	public String getScope() {
		return null;
	}

	public String getClientId() {
		return settingControl
				.getStringValue("oauth." + getName() + ".clientId");
	}

	public String getClientSecret() {
		return settingControl.getStringValue("oauth." + getName()
				+ ".clientSecret");
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && StringUtils.isNotBlank(getClientId());
	}

	protected String getAuthorizationHeaderName() {
		return "Authorization";
	}

	@Override
	protected String getAuthorizationHeaderType() {
		return "Bearer";
	}

	protected String getAccessTokenParameterName() {
		return "oauth_token";
	}

	@PostConstruct
	public void afterPropertiesSet() {
		String clientId = getClientId();
		String clientSecret = getClientSecret();
		if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
			logger.warn(getName() + " clientId or clientSecret is empty");
		}
	}

	@Override
	public String getAuthRedirectURL(HttpServletRequest request,
			String targetUrl) throws Exception {
		OAuth2Token accessToken = restoreToken(request);
		if (accessToken != null) {
			if (accessToken.isExpired()) {
				try {
					accessToken = refreshToken(accessToken);
					saveToken(request, accessToken);
					return targetUrl;
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			} else {
				return targetUrl;
			}
		}
		String state = null;
		if (targetUrl.indexOf('?') > 0) {
			state = targetUrl.substring(targetUrl.lastIndexOf('=') + 1);
			targetUrl = targetUrl.substring(0, targetUrl.indexOf('?'));
		}
		StringBuilder sb = new StringBuilder(getAuthorizeUrl()).append('?')
				.append("client_id").append('=').append(getClientId())
				.append('&').append("redirect_uri").append('=')
				.append(URLEncoder.encode(targetUrl, "UTF-8"));
		sb.append("&response_type=code");
		String scope = getScope();
		if (StringUtils.isNotBlank(scope))
			try {
				sb.append('&').append("scope").append('=')
						.append(URLEncoder.encode(scope, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		if (StringUtils.isNotBlank(state))
			sb.append('&').append("state").append('=').append(state);
		return sb.toString();
	}

	@Override
	public OAuthToken getToken(HttpServletRequest request) throws Exception {
		OAuth2Token accessToken = restoreToken(request);
		if (accessToken != null) {
			if (accessToken.isExpired()) {
				logger.warn("accessToken {} is expired", accessToken);
				try {
					accessToken = refreshToken(accessToken);
					saveToken(request, accessToken);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		if (accessToken == null) {
			String error = request.getParameter("error");
			if (StringUtils.isNotBlank(error)) {
				String errormsg = "error: "
						+ error
						+ ",url: "
						+ request.getRequestURL().append("?")
								.append(request.getQueryString()).toString();
				logger.error(errormsg);
				throw new RuntimeException(errormsg);
			}
			Map<String, String> params = new HashMap<String, String>(8);
			params.put("code", request.getParameter("code"));
			params.put("client_id", getClientId());
			params.put("client_secret", getClientSecret());
			params.put("redirect_uri", request.getRequestURL().toString());
			params.put("grant_type", "authorization_code");
			String content = null;
			try {
				content = HttpClientUtils.postResponseText(
						getAccessTokenEndpoint(), params);
			} catch (Exception e) {
				logger.error(
						getAccessTokenEndpoint() + "," + params.toString(), e);
			}
			accessToken = new OAuth2Token(content);
			if (accessToken.getAccess_token() == null)
				logger.error("access_token is null,and content is {}" + content);
			saveToken(request, accessToken);
		}
		return accessToken;
	}

	@Override
	public Profile getProfile(HttpServletRequest request) throws Exception {
		OAuth2Token accessToken = (OAuth2Token) getToken(request);
		String content = invoke(accessToken.getAccess_token(), getProfileUrl());
		try {
			Profile p = getProfileFromContent(content);
			postProcessProfile(p, accessToken.getAccess_token());
			return p;
		} catch (Exception e) {
			logger.error("content is {}", content);
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	protected void postProcessProfile(Profile p, String accessToken)
			throws Exception {
	}

	public String invoke(HttpServletRequest request, String protectedURL)
			throws Exception {
		OAuth2Token accessToken = restoreToken(request);
		if (accessToken == null)
			throw new IllegalAccessException("must already authorized");
		if (accessToken.isExpired()) {
			try {
				accessToken = refreshToken(accessToken);
				saveToken(request, accessToken);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return invoke(accessToken.getAccess_token(), protectedURL);

	}

	public String invoke(String accessToken, String protectedURL)
			throws Exception {
		Map<String, String> map = new HashMap<String, String>(2, 1);
		if (!isUseAuthorizationHeader())
			map.put(getAccessTokenParameterName(), accessToken);
		else
			map.put(getAuthorizationHeaderName(), getAuthorizationHeaderType()
					+ " " + accessToken);
		return invoke(protectedURL, isUseAuthorizationHeader() ? null : map,
				isUseAuthorizationHeader() ? map : null);
	}

	protected String invoke(String protectedURL, Map<String, String> params,
			Map<String, String> headers) throws IOException {
		return HttpClientUtils.getResponseText(protectedURL, params, headers);
	}

	public OAuth2Token refreshToken(OAuth2Token accessToken) throws Exception {
		Map<String, String> params = new HashMap<String, String>(8);
		params.put("client_id", getClientId());
		params.put("client_secret", getClientSecret());
		params.put("refresh_token", accessToken.getRefresh_token());
		params.put("grant_type", "refresh_token");
		String content = HttpClientUtils.postResponseText(
				getAccessTokenEndpoint(), params);
		try {
			return JsonUtils.fromJson(content, OAuth2Token.class);
		} catch (Exception e) {
			logger.error("refreshToken failed,content is {}" + content);
			return null;
		}
	}

	protected void saveToken(HttpServletRequest request, OAuth2Token token) {
		token.setCreate_time(System.currentTimeMillis());
		request.setAttribute(getName() + "_token", token.getSource());
	}

	protected OAuth2Token restoreToken(HttpServletRequest request)
			throws Exception {
		SecurityContext sc = (SecurityContext) request
				.getSession()
				.getAttribute(
						HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		if (sc != null) {
			Authentication auth = sc.getAuthentication();
			if (auth != null && auth.getPrincipal() instanceof User) {
				OAuth2Token token = (OAuth2Token) OAuthTokenUtils
						.getTokenFromUserAttribute(this,
								(User) auth.getPrincipal());
				if (token != null)
					return token;
			}
		}
		String key = getName() + "_token";
		String source = (String) request.getAttribute(key);
		if (StringUtils.isBlank(source))
			return null;
		return new OAuth2Token(source);
	}

}
