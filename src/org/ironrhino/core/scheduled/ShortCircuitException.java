package org.ironrhino.core.scheduled;

public class ShortCircuitException extends RuntimeException {

	private static final long serialVersionUID = -8469494468457671271L;

	public ShortCircuitException() {

	}

	public ShortCircuitException(String message) {
		super(message);
	}

	public ShortCircuitException(String message, Exception cause) {
		super(message, cause);
	}

}