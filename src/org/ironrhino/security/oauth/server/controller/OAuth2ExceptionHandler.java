package org.ironrhino.security.oauth.server.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.security.oauth.server.component.OAuthErrorHandler;
import org.ironrhino.security.oauth.server.domain.OAuthError;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Component
@ControllerAdvice
public class OAuth2ExceptionHandler extends OAuthErrorHandler {

	@ExceptionHandler(OAuthError.class)
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, OAuthError oauthError)
			throws IOException {
		super.handle(request, response, oauthError);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public void handle(HttpServletRequest request, HttpServletResponse response,
			MissingServletRequestParameterException ex) throws IOException {
		OAuthError error = new OAuthError(OAuthError.INVALID_REQUEST, ex.getMessage());
		handle(request, response, error);
	}

}
