package org.ironrhino.core.spring.security;

import org.springframework.security.authentication.AccountStatusException;

public class CredentialsNeedResetException extends AccountStatusException {

	private static final long serialVersionUID = 1672785423397080411L;

	public CredentialsNeedResetException(String msg) {
		super(msg);
	}

	public CredentialsNeedResetException(String msg, Throwable t) {
		super(msg, t);
	}
}