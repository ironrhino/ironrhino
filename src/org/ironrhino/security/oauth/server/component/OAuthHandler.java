package org.ironrhino.security.oauth.server.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.util.RequestUtils;
import org.ironrhino.core.util.UserAgent;
import org.ironrhino.security.oauth.server.domain.OAuthAuthorization;
import org.ironrhino.security.oauth.server.domain.OAuthError;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.service.OAuthAuthorizationService;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class OAuthHandler extends AccessHandler {

	public static final String REQUEST_ATTRIBUTE_KEY_OAUTH_AUTHORIZATION = "_OAUTH_AUTHORIZATION";
	public static final String SESSION_ID_PREFIX = "tk_";

	@Autowired
	private Logger logger;

	@Value("${oauth.api.pattern:/user/self,/oauth2/tokeninfo,/oauth2/revoketoken,/api/*}")
	private String apiPattern;

	@Value("${oauth.api.excludePattern:}")
	private String apiExcludePattern;

	@Value("${oauth.api.sessionFallback:true}")
	private boolean sessionFallback = true;

	@Autowired
	private OAuthAuthorizationService oauthAuthorizationService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private OAuthErrorHandler oauthErrorHandler;

	@Autowired
	private HttpSessionManager httpSessionManager;

	@Override
	public String getPattern() {
		return apiPattern;
	}

	@Override
	public String getExcludePattern() {
		return apiExcludePattern;
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, String> map = RequestUtils.parseParametersFromQueryString(request.getQueryString());
		String token = map.get("oauth_token");
		if (token == null)
			token = map.get("access_token");
		if (token == null) {
			String header = request.getHeader("Authorization");
			if (header != null) {
				header = header.trim();
				if (header.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
					// oauth 2.0
					token = header.substring("bearer ".length());
				} else if (header.toLowerCase(Locale.ROOT).startsWith("oauth ")) {
					header = header.substring("oauth ".length());
					int i = header.indexOf("oauth_token=");
					if (i < 0) {
						// oauth 2.0
						token = header;
					} else {
						// oauth 1.0
						header = header.substring(header.indexOf("\"", i) + 1);
						token = header.substring(0, header.indexOf("\""));
					}
				} else {
					oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.INVALID_REQUEST,
							"Invalid Authorization header, must starts with OAuth or Bearer"));
					return true;
				}
			}
		}
		if (StringUtils.isBlank(token)) {
			if (sessionFallback) {
				String sessionTracker = RequestUtils.getCookieValue(request,
						httpSessionManager.getSessionTrackerName());
				if (StringUtils.isNotBlank(sessionTracker))
					return false;
			}
			oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.INVALID_TOKEN, "missing_token"));
			return true;
		}

		OAuthAuthorization authorization = oauthAuthorizationService.get(token);
		if (authorization == null) {
			oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.INVALID_TOKEN));
			return true;
		}

		if (authorization.getExpiresIn() < 0) {
			oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.INVALID_TOKEN, "expired_token"));
			return true;
		}

		String[] scopes = null;
		if (StringUtils.isNotBlank(authorization.getScope()))
			scopes = authorization.getScope().split("\\s");
		boolean scopeAuthorized = (scopes == null);
		if (!scopeAuthorized && scopes != null) {
			for (String s : scopes) {
				String requestURL = request.getRequestURL().toString();
				if (requestURL.startsWith(s)) {
					scopeAuthorized = true;
					break;
				}
			}
		}
		if (!scopeAuthorized) {
			oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.INSUFFICIENT_SCOPE));
			return true;
		}

		String clientId = authorization.getClientId();
		if (clientId != null) {
			String clientName = authorization.getClientName();
			if (clientName == null) {
				oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.UNAUTHORIZED_CLIENT));
				return true;
			}
			UserAgent ua = new UserAgent(request.getHeader("User-Agent"));
			ua.setAppId(clientId);
			ua.setAppName(clientName);
			request.setAttribute("userAgent", ua);
		}

		UserDetails ud = null;
		if (authorization.getGrantType() == GrantType.client_credential
				|| authorization.getGrantType() == GrantType.client_credentials) {
			try {
				ud = userDetailsService.loadUserByUsername(authorization.getClientOwner());
			} catch (UsernameNotFoundException unf) {
				logger.error(unf.getMessage(), unf);
			}
			if (ud == null || !ud.isEnabled() || !ud.isAccountNonExpired() || !ud.isAccountNonLocked()) {
				oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.UNAUTHORIZED_CLIENT));
				return true;
			}
		} else if (authorization.getGrantor() != null) {
			try {
				ud = userDetailsService.loadUserByUsername(authorization.getGrantor());
			} catch (UsernameNotFoundException unf) {
				logger.error(unf.getMessage(), unf);
			}
			if (ud == null || !ud.isEnabled() || !ud.isAccountNonExpired() || !ud.isAccountNonLocked()) {
				oauthErrorHandler.handle(request, response, new OAuthError(OAuthError.INVALID_REQUEST, "invalid_user"));
				return true;
			}
		}

		SecurityContext sc = SecurityContextHolder.getContext();
		Authentication auth = new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities());
		sc.setAuthentication(auth);
		MDC.put("username", auth.getName());
		Map<String, Object> sessionMap = new HashMap<>(2, 1);
		sessionMap.put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
		request.setAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API, sessionMap);
		request.setAttribute(REQUEST_ATTRIBUTE_KEY_OAUTH_AUTHORIZATION, authorization);
		request.setAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_KEY_SESSION_ID_FOR_API, SESSION_ID_PREFIX + token);
		return false;

	}

}
