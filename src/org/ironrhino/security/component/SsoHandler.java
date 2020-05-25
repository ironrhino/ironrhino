package org.ironrhino.security.component;

import static org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional.ANY;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.AccessHandler;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.core.util.RequestUtils;
import org.slf4j.MDC;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@ApplicationContextPropertiesConditional(key = "portal.baseUrl", value = ANY)
@Component
@Slf4j
public class SsoHandler extends AccessHandler {

	public static final String REQUEST_ATTRIBUTE_KEY_SSO = "SSO";

	private static final String EXCLUDED_PATTERN = "/setup,/oauth/oauth2/*,/assets/*,/remoting/*";

	@Value("${ssoHandler.pattern:}")
	protected String pattern;

	@Value("${ssoHandler.excludePattern:}")
	protected String excludePattern;

	@Value("${ssoHandler.strictAccess:false}")
	protected boolean strictAccess;

	@Value("${httpSessionManager.sessionTrackerName:" + HttpSessionManager.DEFAULT_SESSION_TRACKER_NAME + "}")
	protected String sessionTrackerName = HttpSessionManager.DEFAULT_SESSION_TRACKER_NAME;

	@Value("${httpSessionManager.sessionCookieName:" + HttpSessionManager.DEFAULT_SESSION_COOKIE_NAME + "}")
	protected String sessionCookieName = HttpSessionManager.DEFAULT_SESSION_COOKIE_NAME;

	@Value("${portal.baseUrl}")
	protected String portalBaseUrl;

	@Value("${portal.api.user.self.url:/api/user/@self}")
	protected String portalApiUserSelfUrl;

	@Value("${portal.login.url:/login}")
	protected String portalLoginUrl;

	@Autowired
	protected UserDetailsService userDetailsService;

	@Autowired(required = false)
	protected RestTemplate restTemplate = new RestTemplate();

	@PostConstruct
	private void init() {
		excludePattern = StringUtils.isBlank(excludePattern) ? EXCLUDED_PATTERN
				: excludePattern + "," + EXCLUDED_PATTERN;
	}

	@Override
	public String getPattern() {
		return pattern;
	}

	@Override
	public String getExcludePattern() {
		return excludePattern;
	}

	@Override
	public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!RequestUtils.isSameOrigin(request.getRequestURL().toString(), portalBaseUrl))
			return strictAccess;
		SecurityContext sc = SecurityContextHolder.getContext();
		Authentication auth = sc.getAuthentication();
		if (auth != null && auth.isAuthenticated())
			return false;
		String token = RequestUtils.getCookieValue(request, sessionTrackerName);
		if (StringUtils.isBlank(token)) {
			redirect(request, response);
			return true;
		} else {
			URI apiUri;
			try {
				apiUri = new URI(portalApiUserSelfUrl.indexOf("://") > 0 ? portalApiUserSelfUrl
						: portalBaseUrl + portalApiUserSelfUrl);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return true;
			}
			StringBuilder cookie = new StringBuilder();
			cookie.append(sessionTrackerName).append("=").append(URLEncoder.encode(token, "UTF-8"));
			String session = RequestUtils.getCookieValue(request, sessionCookieName);
			if (session != null)
				cookie.append("; ").append(sessionCookieName).append("=").append(URLEncoder.encode(session, "UTF-8"));
			RequestEntity<?> requestEntity = RequestEntity.get(apiUri).header("Cookie", cookie.toString())
					.header("X-Real-IP", request.getRemoteAddr()).accept(MediaType.APPLICATION_JSON).build();
			try {
				User userFromApi = exchange(requestEntity);
				if (userFromApi != null) {
					UserDetails ud = map(userFromApi);
					auth = new UsernamePasswordAuthenticationToken(ud, ud.getPassword(), ud.getAuthorities());
					sc.setAuthentication(auth);
					MDC.put("username", auth.getName());
					Map<String, Object> sessionMap = new HashMap<>(2, 1);
					sessionMap.put(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
					request.setAttribute(HttpSessionManager.REQUEST_ATTRIBUTE_KEY_SESSION_MAP_FOR_API, sessionMap);
					request.setAttribute(REQUEST_ATTRIBUTE_KEY_SSO, true);
				}
			} catch (HttpClientErrorException.NotFound e) {
				redirect(request, response);
				return true;
			} catch (HttpClientErrorException e) {
				log.error(e.getMessage(), e);
				try {
					String body = e.getResponseBodyAsString();
					log.error("Received: {}", body);
					JsonNode node = JsonUtils.fromJson(body, JsonNode.class);
					if (node.has("message"))
						throw new ErrorMessage(node.get("message").asText());
				} catch (JsonParseException ex) {
				}
				throw e;
			}
		}
		return false;

	}

	protected void redirect(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StringBuffer sb = request.getRequestURL();
		String queryString = request.getQueryString();
		if (StringUtils.isNotBlank(queryString))
			sb.append("?").append(queryString);
		String targetUrl = sb.toString();
		StringBuilder redirectUrl = new StringBuilder(portalLoginUrl.indexOf("://") > 0 ? "" : portalBaseUrl);
		redirectUrl.append(portalLoginUrl).append(portalLoginUrl.indexOf('?') > 0 ? '&' : '?');
		redirectUrl.append("targetUrl=").append(URLEncoder.encode(targetUrl, "UTF-8"));
		response.sendRedirect(redirectUrl.toString());
	}

	protected User exchange(RequestEntity<?> requestEntity) {
		return restTemplate.exchange(requestEntity, User.class).getBody();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected UserDetails map(User userFromApi) {
		try {
			UserDetails user = userDetailsService.loadUserByUsername(userFromApi.getUsername());
			// reset passwordModifyDate to avoid CredentialsExpiredException
			BeanWrapperImpl bw = new BeanWrapperImpl(user);
			if (bw.isWritableProperty("passwordModifyDate"))
				bw.setPropertyValue("passwordModifyDate", null);
			Collection authorities = user.getAuthorities();
			try {
				Set<String> roles = userFromApi.getRoles();
				if (roles != null && !roles.isEmpty()) {
					List<GrantedAuthority> list = AuthorityUtils
							.createAuthorityList(roles.toArray(new String[roles.size()]));
					for (GrantedAuthority ga : list) {
						if (!authorities.contains(ga))
							authorities.add(ga);
					}
				}
			} catch (UnsupportedOperationException e) {
				log.warn("Can not copy roles from portal server because collection is unmodifiable");
			}
			return user;
		} catch (UsernameNotFoundException e) {
			log.error(e.getMessage());
			throw new ErrorMessage("access.denied");
		}
	}

	@Data
	static class User {
		private String username;
		private String name;
		private String email;
		private String phone;
		private Set<String> roles;
	}

}
