package org.ironrhino.core.security.captcha;

import java.io.Serializable;

public class CaptchaStatus implements Serializable {

	private static final long serialVersionUID = -4204944286894067483L;

	public static final CaptchaStatus EMPTY = new CaptchaStatus(false, 0, 0);

	private final boolean required;

	private final int threshold;

	private final int count;

	public CaptchaStatus(boolean required, int threshold, int count) {
		this.required = required;
		this.threshold = threshold;
		this.count = count;
	}

	public boolean isRequired() {
		return required;
	}

	public int getThreshold() {
		return threshold;
	}

	public int getCount() {
		return count;
	}

	public boolean isFirstReachThreshold() {
		return count > 0 && count == threshold;
	}

}
