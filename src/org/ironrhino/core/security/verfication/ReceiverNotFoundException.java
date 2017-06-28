package org.ironrhino.core.security.verfication;

import org.springframework.security.core.AuthenticationException;

public class ReceiverNotFoundException extends AuthenticationException {

	private static final long serialVersionUID = 137113247989004952L;

	public ReceiverNotFoundException(String msg) {
		super(msg);
	}

	public ReceiverNotFoundException(String msg, Throwable t) {
		super(msg, t);
	}
}
