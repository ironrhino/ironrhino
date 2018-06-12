package org.ironrhino.core.util;

public class LockFailedException extends LocalizedException {

	private static final long serialVersionUID = -2016822227780707626L;

	public LockFailedException(String principal) {
		super(principal);
	}

	public LockFailedException(Exception cause) {
		super(cause);
	}

}