package org.ironrhino.core.util;

public class LimitExceededException extends LocalizedException {

	private static final long serialVersionUID = -237035919695433241L;

	public LimitExceededException(String principal) {
		super(principal);
	}

}