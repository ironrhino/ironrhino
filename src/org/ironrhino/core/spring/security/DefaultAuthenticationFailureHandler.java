package org.ironrhino.core.spring.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("classpath*:resources/spring/applicationContext-security*.xml")
public class DefaultAuthenticationFailureHandler implements
		AuthenticationFailureHandler {

	@Override
	public void onAuthenticationFailure(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException e)
			throws IOException, ServletException {
		request.getSession().removeAttribute(
				WebAttributes.AUTHENTICATION_EXCEPTION);
	}

}
