package org.ironrhino.core.spring.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.web.authentication.WebAuthenticationDetails;

public class DefaultWebAuthenticationDetails extends WebAuthenticationDetails {

	private static final long serialVersionUID = -1899435340020810774L;

	private String verificationCode;

	public DefaultWebAuthenticationDetails(HttpServletRequest request) {
		super(request);
		verificationCode = request.getParameter("verificationCode");
	}

	public String getVerificationCode() {
		return verificationCode;
	}
}