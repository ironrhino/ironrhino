package org.ironrhino.core.security.captcha;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaStatus implements Serializable {

	private static final long serialVersionUID = -4204944286894067483L;

	public static final CaptchaStatus EMPTY = new CaptchaStatus(false, 0, 0);

	private final boolean required;

	private final int threshold;

	private final int count;

	public boolean isFirstReachThreshold() {
		return count > 0 && count == threshold;
	}

}
