package org.ironrhino.core.security.webauthn;

public class AttestationFailedException extends RuntimeException {

	private static final long serialVersionUID = -2029464351909887176L;

	public AttestationFailedException(String message) {
		super(message);
	}

	public AttestationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
