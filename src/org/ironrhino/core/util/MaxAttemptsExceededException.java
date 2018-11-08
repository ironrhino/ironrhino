package org.ironrhino.core.util;

public class MaxAttemptsExceededException extends LocalizedException {

	private static final long serialVersionUID = 7599748876516723063L;

	public MaxAttemptsExceededException(int maxAttempts) {
		this(String.valueOf(maxAttempts));
	}

	private MaxAttemptsExceededException(String maxAttempts) {
		super(maxAttempts);
	}

}