package org.ironrhino.core.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@SpringSecurityEnabled
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {

	@Autowired
	private UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException e) throws IOException, ServletException {
		request.getSession().removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
		String username = request.getParameter(usernamePasswordAuthenticationFilter.getUsernameParameter());
		log.warn("Authenticate \"{}\" failed with {}: {}", username, e.getClass().getSimpleName(), e.getMessage());
	}

}
