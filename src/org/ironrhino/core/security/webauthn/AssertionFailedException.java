package org.ironrhino.core.security.webauthn;

public class AssertionFailedException extends RuntimeException {

	private static final long serialVersionUID = -2029464351909887176L;

	public AssertionFailedException(String message) {
		super(message);
	}

	public AssertionFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
