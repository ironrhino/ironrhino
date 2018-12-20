package org.ironrhino.core.security.otp;

import java.util.Calendar;
import java.util.TimeZone;

public class Totp extends Hotp {

	private static final int TOLERANCE = 1;

	public Totp(String secret) {
		super(secret);
	}

	public Totp(String secret, int digits) {
		super(secret, digits);
	}

	public Totp(String secret, int digits, int period) {
		super(secret, digits, period);
	}

	public Totp(String secret, int digits, int period, String algorithm) {
		super(secret, digits, period, algorithm);
	}

	public String now() {
		return getToken(currentCount());
	}

	public boolean verify(String challenge) {
		long count = currentCount();
		for (int i = 0; i <= TOLERANCE; i++)
			if (verify(count - i, challenge))
				return true;
		return false;
	}

	private long currentCount() {
		return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000 / getPeriod();
	}

}
