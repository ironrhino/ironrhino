package org.ironrhino.core.util;

public class IllegalConcurrentAccessException extends LocalizedException {

	private static final long serialVersionUID = -237035919695433241L;

	public IllegalConcurrentAccessException(String principal) {
		super(principal);
	}

}