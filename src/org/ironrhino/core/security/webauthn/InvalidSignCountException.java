package org.ironrhino.core.security.webauthn;

public class InvalidSignCountException extends RuntimeException {

	private static final long serialVersionUID = -6005646232931355840L;

	public InvalidSignCountException(String message) {
		super(message);
	}

}
