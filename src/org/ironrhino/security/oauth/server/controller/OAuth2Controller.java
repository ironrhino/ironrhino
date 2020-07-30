package org.ironrhino.security.oauth.server.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.jwt.Jwt;
import org.ironrhino.core.security.verfication.VerificationManager;
import org.ironrhino.core.struts.I18N;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.security.oauth.server.component.OAuthHandler;
import org.ironrhino.security.oauth.server.domain.OAuthError;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.event.AuthorizeEvent;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.service.OAuthManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@Validated
@RequestMapping("/oauth2")
public class OAuth2Controller {

	private static final String TOKEN_PATH = "/token";

	@Autowired
	private EventPublisher eventPublisher;

	@Autowired
	private OAuthManager oauthManager;

	@Autowired
	private OAuthHandler oauthHandler;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private AuthenticationFailureHandler authenticationFailureHandler;

	@Autowired
	private AuthenticationSuccessHandler authenticationSuccessHandler;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private WebAuthenticationDetailsSource authenticationDetailsSource;

	@Autowired(required = false)
	private VerificationManager verificationManager;

	@RequestMapping(path = TOKEN_PATH, params = "grant_type=password", method = { RequestMethod.GET,
			RequestMethod.POST })
	public Map<String, Object> password(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String client_id, @RequestParam String client_secret, @RequestParam String username,
			@RequestParam String password, @RequestParam(required = false) String device_id,
			@RequestParam(required = false) String device_name) {
		return exchange(request, response, GrantType.password, client_id, client_secret, username, password, device_id,
				device_name);
	}

	@RequestMapping(path = TOKEN_PATH, params = "grant_type=" + GrantType.JWT_BEARER, method = { RequestMethod.GET,
			RequestMethod.POST })
	public Map<String, Object> jwt_bear(HttpServletRequest request, HttpServletResponse response,
			@RequestParam String client_id, @RequestParam String client_secret, @RequestParam String username,
			@RequestParam String password, @RequestParam(required = false) String device_id,
			@RequestParam(required = false) String device_name) {
		if (oauthHandler == null || !oauthHandler.isJwtEnabled())
			throw new OAuthError(OAuthError.UNSUPPORTED_GRANT_TYPE);
		return exchange(request, response, GrantType.jwt_bearer, client_id, client_secret, username, password,
				device_id, device_name);
	}

	@RequestMapping(path = TOKEN_PATH, params = "grant_type=client_credentials", method = { RequestMethod.GET,
			RequestMethod.POST })
	public Map<String, Object> client_credentials(@RequestParam String client_id, @RequestParam String client_secret,
			@RequestParam(required = false) String device_id, @RequestParam(required = false) String device_name) {
		Client client = new Client();
		client.setId(client_id);
		client.setSecret(client_secret);
		try {
			Authorization authorization = oauthManager.grant(client, device_id, device_name);
			return convert(authorization);
		} catch (Exception e) {
			log.error("Exchange token by client_credentials for \"{}\" failed with {}: {}", client_id,
					e.getClass().getName(), e.getLocalizedMessage());
			throw new OAuthError(OAuthError.INVALID_REQUEST, e.getLocalizedMessage());
		}
	}

	@RequestMapping(path = TOKEN_PATH, params = "grant_type=authorization_code", method = { RequestMethod.GET,
			RequestMethod.POST })
	public Map<String, Object> authorization_code(HttpServletRequest request, @RequestParam String client_id,
			@RequestParam String client_secret, @RequestParam String code,
			@RequestParam(required = false) String redirect_uri) {
		Client client = new Client();
		client.setId(client_id);
		client.setSecret(client_secret);
		client.setRedirectUri(redirect_uri);
		try {
			Authorization authorization = oauthManager.authenticate(code, client);
			eventPublisher.publish(new AuthorizeEvent(authorization.getGrantor(), request.getRemoteAddr(),
					client.getName(), GrantType.authorization_code.name()), Scope.LOCAL);
			return convert(authorization);
		} catch (Exception e) {
			log.error("Exchange token by code for \"{}\" failed with {}: {}", code, e.getClass().getName(),
					e.getLocalizedMessage());
			throw new OAuthError(OAuthError.INVALID_REQUEST, e.getLocalizedMessage());
		}
	}

	@RequestMapping(path = TOKEN_PATH, params = "grant_type=refresh_token", method = { RequestMethod.GET,
			RequestMethod.POST })
	public Map<String, Object> refresh_token(@RequestParam String client_id, @RequestParam String client_secret,
			@RequestParam String refresh_token) {
		Client client = new Client();
		client.setId(client_id);
		client.setSecret(client_secret);
		try {
			Authorization authorization = oauthManager.refresh(client, refresh_token);
			return convert(authorization);
		} catch (Exception e) {
			log.error("Refresh token \"{}\" failed with {}: {}", refresh_token, e.getClass().getName(),
					e.getLocalizedMessage());
			throw new OAuthError(OAuthError.INVALID_REQUEST, e.getLocalizedMessage());
		}
	}

	@RequestMapping(path = TOKEN_PATH, method = { RequestMethod.GET, RequestMethod.POST })
	public void unsupported(@RequestParam GrantType grant_type) {
		throw new OAuthError(OAuthError.UNSUPPORTED_GRANT_TYPE);
	}

	@GetMapping(path = "/info")
	public Map<String, Object> info(@RequestParam String access_token) {
		Authorization authorization = oauthManager.retrieve(access_token);
		if (authorization == null) {
			throw new OAuthError(OAuthError.INVALID_TOKEN);
		} else if (authorization.getExpiresIn() < 0) {
			throw new OAuthError(OAuthError.INVALID_TOKEN, OAuthError.ERROR_EXPIRED_TOKEN);
		} else {
			Map<String, Object> result = new LinkedHashMap<>();
			if (authorization.getClient() != null)
				result.put("client_id", authorization.getClient());
			if (authorization.getGrantor() != null)
				result.put("username", authorization.getGrantor());
			result.put("expires_in", authorization.getExpiresIn());
			if (authorization.getScope() != null)
				result.put("scope", authorization.getScope());
			return result;
		}
	}

	@RequestMapping(path = "/revoke", method = { RequestMethod.GET, RequestMethod.POST })
	public void revoke(@RequestParam String access_token) {
		boolean revoked = oauthManager.revoke(access_token);
		if (!revoked) {
			throw new OAuthError(OAuthError.INVALID_REQUEST, OAuthError.ERROR_REVOKE_FAILED);
		}
	}

	@RequestMapping(path = "/sendVerificationCode", method = { RequestMethod.GET, RequestMethod.POST })
	public Map<String, Object> sendVerificationCode(@RequestParam String client_id, @RequestParam String client_secret,
			@RequestParam String username) {
		Map<String, Object> result = null;
		if (verificationManager == null) {
			result = new LinkedHashMap<>();
			result.put("code", "2");
			result.put("status", "FORBIDDEN");
			return result;
		}
		try {
			Client client = oauthManager.findClientById(client_id);
			if (client == null)
				throw new OAuthError(OAuthError.INVALID_CLIENT, OAuthError.ERROR_CLIENT_ID_NOT_EXISTS);
			if (!client.getSecret().equals(client_secret))
				throw new OAuthError(OAuthError.INVALID_CLIENT, OAuthError.ERROR_CLIENT_SECRET_MISMATCH);
			if (StringUtils.isNotBlank(username)) {
				verificationManager.send(username);
				result = new LinkedHashMap<>();
				result.put("code", "0");
				result.put("status", "OK");
			}
		} catch (Exception e) {
			log.error("Send verification code to \"{}\" failed with {}: {}", username, e.getClass().getName(),
					e.getLocalizedMessage());
			throw new OAuthError(OAuthError.INVALID_REQUEST, e.getLocalizedMessage());
		}
		return result;
	}

	private Map<String, Object> exchange(HttpServletRequest request, HttpServletResponse response, GrantType grant_type,
			String client_id, String client_secret, String username, String password, String device_id,
			String device_name) {
		Client client = oauthManager.findClientById(client_id);
		if (client == null)
			throw new OAuthError(OAuthError.INVALID_CLIENT, OAuthError.ERROR_CLIENT_ID_NOT_EXISTS);
		if (!client.getSecret().equals(client_secret))
			throw new OAuthError(OAuthError.INVALID_CLIENT, OAuthError.ERROR_CLIENT_SECRET_MISMATCH);
		try {
			UsernamePasswordAuthenticationToken attempt = new UsernamePasswordAuthenticationToken(username, password);
			attempt.setDetails(authenticationDetailsSource.buildDetails(request));
			try {
				Authentication authResult = authenticationManager.authenticate(attempt);
				if (authResult != null)
					authenticationSuccessHandler.onAuthenticationSuccess(request, response, authResult);
			} catch (InternalAuthenticationServiceException failed) {
				throw new IllegalArgumentException(ExceptionUtils.getRootMessage(failed));
			} catch (AuthenticationException failed) {
				authenticationFailureHandler.onAuthenticationFailure(request, response, failed);
				throw new IllegalArgumentException(I18N.getText(failed.getClass().getName()), failed);
			}
			UserDetails ud = userDetailsService.loadUserByUsername(username);
			eventPublisher.publish(
					new AuthorizeEvent(ud.getUsername(), request.getRemoteAddr(), client.getName(), grant_type.name()),
					Scope.LOCAL);
			if (grant_type == GrantType.jwt_bearer) {
				Map<String, Object> result = new LinkedHashMap<>();
				int expiresIn = oauthHandler.getJwtExpiresIn();
				String jwt = Jwt.createWithSubject(ud.getUsername(), ud.getPassword(), expiresIn);
				result.put("access_token", jwt);
				if (expiresIn > 0)
					result.put("expires_in", expiresIn);
				return result;
			} else {
				Authorization authorization = oauthManager.grant(client, ud.getUsername(), device_id, device_name);
				return convert(authorization);
			}
		} catch (Exception e) {
			if (e.getCause() instanceof AuthenticationException)
				log.error("Exchange token by password for \"{}\" failed with {}: {}", username, e.getClass().getName(),
						e.getLocalizedMessage());
			else
				log.error(e.getMessage(), e);
			throw new OAuthError(OAuthError.INVALID_REQUEST, e.getLocalizedMessage());
		}
	}

	private Map<String, Object> convert(Authorization authorization) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("access_token", authorization.getAccessToken());
		result.put("refresh_token", authorization.getRefreshToken());
		result.put("expires_in", authorization.getExpiresIn());
		return result;
	}

}
