package org.ironrhino.security.oauth.client.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.HttpClientUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.client.model.OAuth1Token;
import org.ironrhino.security.oauth.client.model.OAuthToken;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.util.OAuthTokenUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

public abstract class OAuth1Provider extends AbstractOAuthProvider {

	public abstract String getRequestTokenUrl();

	public abstract String getAuthorizeUrl();

	public abstract String getAccessTokenUrl();

	@Override
	public String getVersion() {
		return "v1";
	}

	public String getRealm() {
		return null;
	}

	public boolean isIncludeCallbackWhenRequestToken() {
		return false;
	}

	public String getConsumerKey() {
		return settingControl.getStringValue("oauth." + getName()
				+ ".consumerKey");
	}

	public String getConsumerSecret() {
		return settingControl.getStringValue("oauth." + getName()
				+ ".consumerSecret");
	}

	@Override
	public boolean isEnabled() {
		return super.isEnabled() && StringUtils.isNotBlank(getConsumerKey());
	}

	@PostConstruct
	public void afterPropertiesSet() {
		String key = getConsumerKey();
		String secret = getConsumerSecret();
		if (StringUtils.isEmpty(key) || StringUtils.isEmpty(secret))
			logger.warn(getName() + " key or secret is empty");
	}

	@Override
	@SuppressWarnings("unchecked")
	public String getAuthRedirectURL(HttpServletRequest request,
			String targetUrl) throws Exception {
		OAuth1Token accessToken = restoreToken(request, "access");
		if (accessToken != null)
			return targetUrl;

		String responseBody = null;
		Map<String, String> map = null;

		if (isIncludeCallbackWhenRequestToken()) {
			map = new HashMap<String, String>(2, 1);
			map.put("oauth_callback", targetUrl);
		}
		if (isUseAuthorizationHeader()) {
			Map<String, String> headers = getAuthorizationHeaders("GET",
					getRequestTokenUrl(), map, getRealm(), "", "");
			responseBody = HttpClientUtils.getResponseText(
					getRequestTokenUrl(), map, headers);
		} else {
			Map<String, String> params = getOAuthParams("GET",
					getRequestTokenUrl(), map, "", "");
			responseBody = HttpClientUtils.getResponseText(
					getRequestTokenUrl(), params, Collections.EMPTY_MAP);
		}
		OAuth1Token requestToken = null;
		try {
			requestToken = new OAuth1Token(responseBody);
		} catch (Exception e) {
			logger.error("content is {}", responseBody);
		}
		if (requestToken == null)
			throw new IllegalArgumentException(
					"requestToken is null,responseBody : " + responseBody);
		saveToken(request, requestToken, "request");
		StringBuilder sb = new StringBuilder(getAuthorizeUrl());
		sb.append(sb.indexOf("?") > 0 ? '&' : '?').append("oauth_token")
				.append('=').append(requestToken.getToken());
		if (!isIncludeCallbackWhenRequestToken())
			sb.append('&').append("oauth_callback").append("=")
					.append(URLEncoder.encode(targetUrl, "UTF-8"));
		return sb.toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public OAuthToken getToken(HttpServletRequest request) throws Exception {
		OAuth1Token accessToken = restoreToken(request, "access");
		if (accessToken == null) {
			OAuth1Token requestToken = restoreToken(request, "request");
			removeToken(request, "request");
			String oauth_verifier = request.getParameter("oauth_verifier");
			Map<String, String> map = new HashMap<String, String>(4, 1);
			map.put("oauth_token", requestToken.getToken());
			if (oauth_verifier != null)
				map.put("oauth_verifier", oauth_verifier);
			String responseBody = null;
			if (isUseAuthorizationHeader()) {
				Map<String, String> headers = getAuthorizationHeaders("GET",
						getAccessTokenUrl(), map, getRealm(),
						requestToken.getToken(), requestToken.getSecret());
				responseBody = HttpClientUtils.getResponseText(
						getAccessTokenUrl(), null, headers);
			} else {
				Map<String, String> params = getOAuthParams("GET",
						getAccessTokenUrl(), map, requestToken.getToken(),
						requestToken.getSecret());
				responseBody = HttpClientUtils.getResponseText(
						getAccessTokenUrl(), params, Collections.EMPTY_MAP);
			}
			accessToken = new OAuth1Token(responseBody);
			saveToken(request, accessToken, "access");
		}
		return accessToken;
	}

	@Override
	public Profile getProfile(HttpServletRequest request) throws Exception {
		OAuth1Token accessToken = (OAuth1Token) getToken(request);
		String content = invoke(accessToken, getProfileUrl());
		try {
			Profile p = getProfileFromContent(content);
			postProcessProfile(p, accessToken);
			return p;
		} catch (Exception e) {
			logger.error("content is {}", content);
			logger.error(e.getMessage(), e);
			return null;
		}
	}

	protected void postProcessProfile(Profile p, OAuth1Token accessToken)
			throws Exception {
	}

	public String invoke(HttpServletRequest request, String protectedURL)
			throws Exception {
		OAuth1Token accessToken = restoreToken(request, "access");
		if (accessToken == null)
			throw new IllegalAccessException("must already authorized");
		return invoke(accessToken, protectedURL);

	}

	public String invoke(OAuth1Token accessToken, String protectedURL)
			throws Exception {
		Map<String, String> map;

		if (isUseAuthorizationHeader()) {
			map = getAuthorizationHeaders("GET", protectedURL, null,
					getRealm(), accessToken.getToken(), accessToken.getSecret());
		} else {
			map = getOAuthParams("GET", protectedURL, null,
					accessToken.getToken(), accessToken.getSecret());
		}
		return invoke(protectedURL, isUseAuthorizationHeader() ? null : map,
				isUseAuthorizationHeader() ? map : null);
	}

	protected String invoke(String protectedURL, Map<String, String> params,
			Map<String, String> headers) throws IOException {
		return HttpClientUtils.getResponseText(protectedURL, params, headers);
	}

	protected void saveToken(HttpServletRequest request, OAuth1Token token,
			String type) {
		request.setAttribute(tokenSessionKey(type), token.getSource());
	}

	protected void removeToken(HttpServletRequest request, String type) {
		request.removeAttribute(tokenSessionKey(type));
	}

	protected OAuth1Token restoreToken(HttpServletRequest request, String type)
			throws Exception {
		if (type.equals("access")) {
			SecurityContext sc = (SecurityContext) request
					.getSession()
					.getAttribute(
							HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
			if (sc != null) {
				Authentication auth = sc.getAuthentication();
				if (auth != null && auth.getPrincipal() instanceof User) {
					OAuth1Token token = (OAuth1Token) OAuthTokenUtils
							.getTokenFromUserAttribute(this,
									(User) auth.getPrincipal());
					if (token != null)
						return token;
				}
			}
		}
		String source = (String) request.getAttribute(tokenSessionKey(type));
		if (StringUtils.isBlank(source))
			return null;
		return new OAuth1Token(source);
	}

	private String tokenSessionKey(String type) {
		return new StringBuffer(getName()).append('_').append(type)
				.append("_token").toString();
	}

	protected Map<String, String> getAuthorizationHeaders(String method,
			String url, Map<String, String> params, String realm, String token,
			String tokenSecret) {
		Map<String, String> oauthparams = getOAuthParams(method, url, params,
				token, tokenSecret);
		StringBuilder sb = new StringBuilder();
		sb.append(getAuthorizationHeaderType());
		sb.append(" realm=\"");
		if (realm != null)
			sb.append(realm);
		sb.append("\",");
		for (Map.Entry<String, String> entry : oauthparams.entrySet())
			sb.append(entry.getKey()).append("=").append("\"")
					.append(Utils.percentEncode(entry.getValue()))
					.append("\",");
		sb.deleteCharAt(sb.length() - 1);
		Map<String, String> map = new HashMap<String, String>(2, 1);
		map.put("Authorization", sb.toString());
		return map;
	}

	protected String addOAuthQueryString(String method, String url,
			Map<String, String> params, String realm, String token,
			String tokenSecret) {
		Map<String, String> oauthparams = getOAuthParams(method, url, params,
				token, tokenSecret);
		StringBuilder sb = new StringBuilder();
		sb.append(url).append(url.indexOf('?') > 0 ? '&' : '?');
		for (Map.Entry<String, String> entry : oauthparams.entrySet())
			sb.append(entry.getKey()).append("=").append(entry.getValue())
					.append('&');
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	protected Map<String, String> getOAuthParams(String method, String url,
			Map<String, String> params, String token, String tokenSecret) {
		Map<String, String> oauthparams = new HashMap<String, String>();
		oauthparams.put("oauth_consumer_key", getConsumerKey());
		oauthparams.put("oauth_timestamp", Utils.getTimestamp());
		oauthparams.put("oauth_nonce", Utils.getNonce());
		if (StringUtils.isNotBlank(token))
			oauthparams.put("oauth_token", token);
		oauthparams.put("oauth_signature_method", "HMAC-SHA1");
		oauthparams.put("oauth_version", "1.0");
		Map<String, String> temp = new HashMap<String, String>();
		if (params != null) {
			temp.putAll(params);
			for (Map.Entry<String, String> entry : params.entrySet())
				if (entry.getKey().startsWith("oauth_"))
					oauthparams.put(entry.getKey(), entry.getValue());

			Iterator<String> iterator = params.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				if (key.startsWith("oauth_")) {
					iterator.remove();
					params.remove(key);
				}
			}
		}
		temp.putAll(oauthparams);
		try {
			String baseString = Utils.getBaseString(method, url, temp);
			String signature = Utils.getSignature(baseString,
					getConsumerSecret(), tokenSecret);
			oauthparams.put("oauth_signature", signature);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return oauthparams;
	}

	private static class Utils {

		public static String getTimestamp() {
			return String.valueOf((int) (System.currentTimeMillis() / 1000));
		}

		public static String getNonce() {
			return CodecUtils.randomString(8).toLowerCase();
		}

		public static String percentEncode(String string) {
			try {
				return URLEncoder.encode(string, "UTF-8")
						.replaceAll("\\+", "%20").replaceAll("\\*", "%2A")
						.replaceAll("%7E", "~");
			} catch (Exception e) {
				e.printStackTrace();
				return string;
			}
		}

		public static String getSignature(String baseString,
				String consumerSecret, String tokenSecret) throws Exception {

			SecretKeySpec key = new SecretKeySpec(
					(percentEncode(consumerSecret) + '&' + percentEncode(tokenSecret))
							.getBytes("UTF-8"), "HmacSHA1");
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(key);
			byte[] bytes = mac.doFinal(baseString.getBytes("UTF-8"));
			return new String(Base64.encodeBase64(bytes)).replace("\r\n", "");
		}

		public static String getBaseString(String method, String url,
				Map<String, String> params) throws Exception {
			StringBuilder sb = new StringBuilder();
			sb.append(method.toUpperCase()).append('&');
			URL u = new URL(url);
			StringBuilder usb = new StringBuilder();
			usb.append(u.getProtocol().toLowerCase()).append("://")
					.append(u.getHost().toLowerCase());
			if (u.getPort() > 0 && u.getPort() != u.getDefaultPort())
				usb.append(":").append(u.getPort());
			usb.append(u.getPath());
			String queryString = u.getQuery();
			if (StringUtils.isNotBlank(queryString)) {
				String[] arr = queryString.split("&");
				for (String s : arr) {
					int i = s.indexOf('=');
					if (i > 0) {
						params.put(s.substring(0, i), s.substring(i + 1));
					} else {
						params.put(s, "");
					}
				}
			}
			sb.append(percentEncode(usb.toString())).append('&');
			TreeMap<String, String> map = new TreeMap<String, String>();
			map.putAll(params);
			StringBuilder psb = new StringBuilder();
			for (Map.Entry<String, String> entry : map.entrySet())
				psb.append('&').append(percentEncode(entry.getKey()))
						.append('=').append(percentEncode(entry.getValue()));
			psb.deleteCharAt(0);
			sb.append(percentEncode(psb.toString()));
			return sb.toString();
		}

	}

}
