package org.ironrhino.core.spring.security;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Autowired
	private Logger logger;

	@Autowired
	private UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Autowired
	private CacheManager cacheManager;

	@Value("${authenticationFailureHandler.delayNamespace:username}")
	private String delayNamespace;

	@Value("${authenticationFailureHandler.delayInterval:5}")
	private int delayInterval;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException e) throws IOException, ServletException {
		request.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		String username = request.getParameter(usernamePasswordAuthenticationFilter.getUsernameParameter());
		logger.warn("Authenticate \"{}\" failed with {}: {}", username, e.getClass().getSimpleName(), e.getMessage());
		if (e instanceof BadCredentialsException) {
			if (username != null)
				cacheManager.delay(username, delayNamespace, delayInterval, TimeUnit.SECONDS, delayInterval / 2);
		}
	}

}
