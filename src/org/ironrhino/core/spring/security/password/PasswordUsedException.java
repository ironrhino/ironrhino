package org.ironrhino.core.spring.security.password;

import org.springframework.security.core.AuthenticationException;

public class PasswordUsedException extends AuthenticationException {

	private static final long serialVersionUID = 137113247989004952L;

	public PasswordUsedException(String msg) {
		super(msg);
	}

	public PasswordUsedException(String msg, Throwable t) {
		super(msg, t);
	}
}
