package org.ironrhino.core.security.otp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

import lombok.Getter;

@Getter
public class Hotp {

	public static Base32 BASE32 = new Base32(false, (byte) 0);

	public static final int DEFAULT_DIGITS = 6;
	public static final int DEFAULT_PERIOD = 30;
	public static final String DEFAULT_ALGORITHM = "HmacSHA1";

	private final String secret;
	private final int digits;
	private final int period;
	private final String algorithm; // "HmacSHA1" "HmacSHA256" "HmacSHA512"

	public Hotp(String secret) {
		this.secret = secret;
		this.digits = DEFAULT_DIGITS;
		this.period = DEFAULT_PERIOD;
		this.algorithm = DEFAULT_ALGORITHM;
	}

	public Hotp(String secret, int digits) {
		this.secret = secret;
		this.digits = digits;
		this.period = DEFAULT_PERIOD;
		this.algorithm = DEFAULT_ALGORITHM;
	}

	public Hotp(String secret, int digits, int period) {
		this.secret = secret;
		this.digits = digits;
		this.period = period;
		this.algorithm = DEFAULT_ALGORITHM;
	}

	public Hotp(String secret, int digits, int period, String algorithm) {
		this.secret = secret;
		this.digits = digits;
		this.period = period;
		if (algorithm.toUpperCase(Locale.ROOT).startsWith("SHA"))
			algorithm = "Hmac" + algorithm;
		this.algorithm = algorithm;
	}

	public String uri(String name) {
		return uri(name, null);
	}

	public String uri(String name, String issuer) {
		// https://github.com/google/google-authenticator/wiki/Key-Uri-Format
		try {
			StringBuilder sb = new StringBuilder("otpauth://")
					.append(getClass().getSimpleName().toLowerCase(Locale.ROOT)).append('/');
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

	public String getToken(long count) {
		try {
			Mac mac = Mac.getInstance(algorithm);
			mac.init(new SecretKeySpec(BASE32.decode(secret), "RAW"));
			byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(count).array());
			int offset = hash[hash.length - 1] & 0xf;
			int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
					| ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);
			return String.format("%0" + digits + "d", binary % (int) Math.pow(10, digits));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean verify(long count, String challenge) {
		return getToken(count).equals(challenge);
	}

}
