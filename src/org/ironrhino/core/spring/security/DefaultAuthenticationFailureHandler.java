package org.ironrhino.core.spring.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.throttle.ThrottleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
@Slf4j
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Autowired
	private UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	private ThrottleService throttleService;

	@Value("${authenticationFailureHandler.delayInterval:5}")
	private int delayInterval;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException e) throws IOException, ServletException {
		request.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		String username = request.getParameter(usernamePasswordAuthenticationFilter.getUsernameParameter());
		log.warn("Authenticate \"{}\" failed with {}: {}", username, e.getClass().getSimpleName(), e.getMessage());
		if (e instanceof BadCredentialsException) {
			if (username != null)
				throttleService.delay("username:" + username, delayInterval, TimeUnit.SECONDS, delayInterval / 2);
		}
	}

}
