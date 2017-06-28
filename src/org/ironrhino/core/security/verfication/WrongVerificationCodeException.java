package org.ironrhino.core.security.verfication;

import org.springframework.security.core.AuthenticationException;

public class WrongVerificationCodeException extends AuthenticationException {

	private static final long serialVersionUID = 137113247989004952L;

	public WrongVerificationCodeException(String msg) {
		super(msg);
	}

	public WrongVerificationCodeException(String msg, Throwable t) {
		super(msg, t);
	}
}
