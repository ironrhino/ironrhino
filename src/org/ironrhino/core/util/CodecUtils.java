package org.ironrhino.core.util;

import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.codec.binary.Hex;
import org.ironrhino.core.tracing.Tracing;

import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CodecUtils {

	public static final String DEFAULT_ENCODING = "UTF-8";

	public static final char[] CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private static final Random random = new Random();

	private static ThreadLocal<SoftReference<MessageDigest>> MD5 = new ThreadLocal<SoftReference<MessageDigest>>() {

		@Override
		protected SoftReference<MessageDigest> initialValue() {
			try {
				return new SoftReference<>(MessageDigest.getInstance("MD5"));
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("md5 algorythm found");
			}
		}
	};

	private static ThreadLocal<SoftReference<MessageDigest>> SHA = new ThreadLocal<SoftReference<MessageDigest>>() {

		@Override
		protected SoftReference<MessageDigest> initialValue() {
			try {
				return new SoftReference<>(MessageDigest.getInstance("SHA"));
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("sha algorythm found");
			}
		}
	};

	private static ThreadLocal<SoftReference<MessageDigest>> SHA256 = new ThreadLocal<SoftReference<MessageDigest>>() {

		@Override
		protected SoftReference<MessageDigest> initialValue() {
			try {
				return new SoftReference<>(MessageDigest.getInstance("SHA-256"));
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("sha algorythm found");
			}
		}
	};

	public static byte[] md5(byte[] input) {
		SoftReference<MessageDigest> instanceRef = MD5.get();
		MessageDigest md5;
		if (instanceRef == null || (md5 = instanceRef.get()) == null) {
			try {
				md5 = MessageDigest.getInstance("MD5");
				instanceRef = new SoftReference<>(md5);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("md5 algorythm found");
			}
			MD5.set(instanceRef);
		}
		md5.reset();
		md5.update(input);
		return md5.digest();
	}

	public static byte[] md5(String input) {
		return md5(input, DEFAULT_ENCODING);
	}

	public static byte[] md5(String input, String encoding) {
		try {
			return md5(input.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static String md5Hex(byte[] input) {
		return Hex.encodeHexString(md5(input));
	}

	public static String md5Hex(String input) {
		return md5Hex(input, DEFAULT_ENCODING);
	}

	public static String md5Hex(String input, String encoding) {
		try {
			return md5Hex(input.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static byte[] sha(byte[] input) {
		SoftReference<MessageDigest> instanceRef = SHA.get();
		MessageDigest sha;
		if (instanceRef == null || (sha = instanceRef.get()) == null) {
			try {
				sha = MessageDigest.getInstance("SHA");
				instanceRef = new SoftReference<>(sha);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("sha algorythm found");
			}
			SHA.set(instanceRef);
		}
		sha.reset();
		sha.update(input);
		return sha.digest();
	}

	public static byte[] sha(String input) {
		return sha(input, DEFAULT_ENCODING);
	}

	public static byte[] sha(String input, String encoding) {
		try {
			return sha(input.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static String shaHex(byte[] input) {
		return Hex.encodeHexString(sha(input));
	}

	public static String shaHex(String input) {
		return shaHex(input, DEFAULT_ENCODING);
	}

	public static String shaHex(String input, String encoding) {
		try {
			return shaHex(input.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static byte[] sha256(byte[] input) {
		SoftReference<MessageDigest> instanceRef = SHA256.get();
		MessageDigest sha256;
		if (instanceRef == null || (sha256 = instanceRef.get()) == null) {
			try {
				sha256 = MessageDigest.getInstance("SHA-256");
				instanceRef = new SoftReference<>(sha256);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalStateException("sha-256 algorythm found");
			}
			SHA256.set(instanceRef);
		}
		sha256.reset();
		sha256.update(input);
		return sha256.digest();
	}

	public static byte[] sha256(String input) {
		return sha256(input, DEFAULT_ENCODING);
	}

	public static byte[] sha256(String input, String encoding) {
		try {
			return sha256(input.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static String sha256Hex(byte[] input) {
		return Hex.encodeHexString(sha256(input));
	}

	public static String sha256Hex(String input) {
		return sha256Hex(input, DEFAULT_ENCODING);
	}

	public static String sha256Hex(String input, String encoding) {
		try {
			return sha256Hex(input.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	public static String fuzzify(String input) {
		try {
			byte[] bytes = Base64.getEncoder().withoutPadding().encode(input.getBytes(DEFAULT_ENCODING));
			swap(bytes);
			return new String(bytes);
		} catch (UnsupportedEncodingException e) {
			return input;
		}
	}

	public static String defuzzify(String input) {
		try {
			byte[] bytes = input.getBytes();
			swap(bytes);
			return new String(Base64.getDecoder().decode(bytes), DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException e) {
			return input;
		}
	}

	private static void swap(byte[] bytes) {
		int half = bytes.length / 2;
		for (int i = 0; i < half; i++) {
			byte temp = bytes[i];
			bytes[i] = bytes[half + i];
			bytes[half + i] = temp;
		}
	}

	public static String swap(String str) {
		char[] chars = str.toCharArray();
		int half = chars.length / 2;
		for (int i = 0; i < half; i++) {
			char temp = chars[i];
			chars[i] = chars[half + i];
			chars[half + i] = temp;
		}
		return new String(chars);
	}

	@Deprecated
	public static String digest(String input) {
		if (input == null)
			return null;
		if (input.length() > 255)
			return input; // avoid long password DOS attack
		boolean isShaInput = input.length() == 40 && input.matches("\\p{XDigit}+");
		return md5Hex(shaHex(input, isShaInput ? 2 : 3));
	}

	public static String md5Hex(String input, int times) {
		for (int i = 0; i < times; i++)
			input = md5Hex(input);
		return input;
	}

	public static String shaHex(String input, int times) {
		for (int i = 0; i < times; i++)
			input = shaHex(input);
		return input;
	}

	private static String string = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	public static String randomString(int digits) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < digits; i++)
			sb.append(string.charAt(ThreadLocalRandom.current().nextInt(26)));
		return sb.toString();
	}

	public static String randomDigitalString(int digits) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < digits; i++)
			sb.append(ThreadLocalRandom.current().nextInt(10));
		return sb.toString();
	}

	public static int randomInt(int digits) {
		if (digits <= 1)
			return ThreadLocalRandom.current().nextInt(10);
		int n = 10;
		for (int i = 1; i < digits - 1; i++)
			n *= 10;
		return n + ThreadLocalRandom.current().nextInt(n * 9);
	}

	public static String nextId() {
		return nextId(22); // back compatibility
	}

	public static String nextId(int length) {
		char[] chars = new char[length];
		chars[0] = CHARS[random.nextInt(length == 22 ? 8 : CHARS.length)];
		for (int i = 1; i < chars.length; i++)
			chars[i] = CHARS[random.nextInt(CHARS.length)];
		return new String(chars);
	}

	@Deprecated
	public static String nextId(String salt) {
		return nextId(32);
	}

	public static String generateRequestId() {
		if (Tracing.isEnabled()) {
			Span span = GlobalTracer.get().activeSpan();
			if (span != null)
				return span.context().toTraceId();
		}
		return nextId();
	}

}
