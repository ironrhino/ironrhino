package org.ironrhino.core.throttle;

import org.ironrhino.core.util.LimitExceededException;

public class FrequencyLimitExceededException extends LimitExceededException {

	private static final long serialVersionUID = 5054274655424477029L;

	public FrequencyLimitExceededException(String principal) {
		super(principal);
	}

}