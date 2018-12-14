package org.ironrhino.core.security.otp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.ironrhino.core.util.CodecUtils;

public class Totp {

	public static Base32 BASE32 = new Base32(false, (byte) 0);

	public static final int DEFAULT_DIGITS = 6;
	public static final int DEFAULT_PERIOD = 30;
	public static final String DEFAULT_ALGORITHM = "HmacSHA1";

	private static final int TOLERANCE = 1;
	private final String secret;
	private final int digits;
	private final int period;
	private final String algorithm; // "HmacSHA1" "HmacSHA256" "HmacSHA512"

	public Totp(String secret) {
		this.secret = secret;
		this.digits = DEFAULT_DIGITS;
		this.period = DEFAULT_PERIOD;
		this.algorithm = DEFAULT_ALGORITHM;
	}

	public Totp(String secret, int digits) {
		this.secret = secret;
		this.digits = digits;
		this.period = DEFAULT_PERIOD;
		this.algorithm = DEFAULT_ALGORITHM;
	}

	public Totp(String secret, int digits, int period) {
		this.secret = secret;
		this.digits = digits;
		this.period = period;
		this.algorithm = DEFAULT_ALGORITHM;
	}

	public Totp(String secret, int digits, int period, String algorithm) {
		this.secret = secret;
		this.digits = digits;
		this.period = period;
		this.algorithm = algorithm;
	}

	public String uri(String name) {
		return uri(name, null);
	}

	public String uri(String name, String issuer) {
		// https://github.com/google/google-authenticator/wiki/Key-Uri-Format
		try {
			StringBuilder sb = new StringBuilder("otpauth://totp/");
			sb.append(URLEncoder.encode(name, "UTF-8"));
			sb.append('?');
			if (issuer != null)
				sb.append("issuer=").append(URLEncoder.encode(issuer, "UTF-8")).append("&");
			sb.append(String.format("secret=%s&algorithm=%s&digits=%d&period=%d", secret, algorithm.substring(4),
					digits, period));
			return sb.toString();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public String now() {
		try {
			return String.format("%0" + digits + "d", otp(currentInterval()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean verify(String verificationCode) {
		try {
			long currentInterval = currentInterval();
			for (int i = TOLERANCE; i >= 0; --i)
				if (Long.valueOf(verificationCode) == otp(currentInterval - i))
					return true;
		} catch (Exception e) {
		}
		return false;
	}

	private long currentInterval() {
		return Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() / 1000 / period;
	}

	private int otp(long interval) throws Exception {
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(BASE32.decode(secret), "RAW"));
		byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(interval).array());
		int offset = hash[hash.length - 1] & 0xf;
		int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
				| ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
		return binary % (int) Math.pow(10, digits);
	}

	public static Totp fromCredentials(String credentials) {
		return new Totp(BASE32.encodeToString(CodecUtils.sha256(credentials)));
	}

}
