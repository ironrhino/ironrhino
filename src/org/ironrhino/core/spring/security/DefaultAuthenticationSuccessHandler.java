package org.ironrhino.core.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
@Setter
@ConfigurationProperties(prefix = "authentication-success-handler")
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	public final static String COOKIE_NAME_LOGIN_USER = "U";

	private boolean usernameInCookie = true;

	private int usernameInCookieMaxAge = 31536000;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws ServletException, IOException {
		if (usernameInCookie && request.isRequestedSessionIdFromCookie()
				&& request.getAttribute("_OAUTH_REQUEST") == null) {
			RequestUtils.saveCookie(request, response, COOKIE_NAME_LOGIN_USER, authentication.getName(),
					usernameInCookieMaxAge, false, false);
		}
	}

}
