package org.ironrhino.core.security.util;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.CodecUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RC4 {
	private static Logger logger = LoggerFactory.getLogger(RC4.class);

	public static final String DEFAULT_KEY_LOCATION = "/resources/key/rc4";
	public static final String KEY_DIRECTORY = "/key/";

	private static final ThreadLocal<SoftReference<RC4>> pool = new ThreadLocal<SoftReference<RC4>>() {
		@Override
		protected SoftReference<RC4> initialValue() {
			return new SoftReference<>(new RC4());
		}
	};

	private static String defaultKey;

	static {
		String s = System.getProperty(AppInfo.getAppName() + ".rc4");
		if (StringUtils.isNotBlank(s)) {
			defaultKey = s;
			logger.info("using system property " + AppInfo.getAppName() + ".rc4 as default key");
		} else {
			try {
				File file = new File(AppInfo.getAppHome() + KEY_DIRECTORY + "rc4");
				if (file.exists()) {
					defaultKey = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
					logger.info("using file " + file.getAbsolutePath());
				} else {
					if (AppInfo.getStage() == Stage.PRODUCTION)
						logger.warn("file " + file.getAbsolutePath()
								+ " doesn't exists, please use your own default key in production!");
					if (RC4.class.getResource(DEFAULT_KEY_LOCATION) != null) {
						try (InputStream is = RC4.class.getResourceAsStream(DEFAULT_KEY_LOCATION)) {
							defaultKey = IOUtils.toString(is, StandardCharsets.UTF_8);
							logger.info("using classpath resource "
									+ RC4.class.getResource(DEFAULT_KEY_LOCATION).toString() + " as default key");
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		if (defaultKey == null)
			defaultKey = AppInfo.getAppName() + ':' + AppInfo.getAppBasePackage();
		defaultKey = CodecUtils.fuzzify(defaultKey);
	}

	private byte state[] = new byte[256];
	private int x;
	private int y;

	public RC4() {
		this(defaultKey);
	}

	public RC4(String key) {
		this(key.getBytes());
	}

	public RC4(byte[] key) {
		for (int i = 0; i < 256; i++) {
			state[i] = (byte) i;
		}
		x = 0;
		y = 0;
		int index1 = 0;
		int index2 = 0;
		byte tmp;
		if (key == null || key.length == 0) {
			throw new NullPointerException();
		}
		for (int i = 0; i < 256; i++) {
			index2 = ((key[index1] & 0xff) + (state[i] & 0xff) + index2) & 0xff;
			tmp = state[i];
			state[i] = state[index2];
			state[index2] = tmp;
			index1 = (index1 + 1) % key.length;
		}
	}

	public byte[] rc4(String data) {
		if (data == null) {
			return null;
		}
		byte[] tmp = data.getBytes();
		this.rc4(tmp);
		return tmp;
	}

	public byte[] rc4(byte[] buf) {
		int xorIndex;
		byte tmp;
		if (buf == null) {
			return null;
		}
		byte[] result = new byte[buf.length];
		for (int i = 0; i < buf.length; i++) {
			x = (x + 1) & 0xff;
			y = ((state[x] & 0xff) + y) & 0xff;
			tmp = state[x];
			state[x] = state[y];
			state[y] = tmp;
			xorIndex = ((state[x] & 0xff) + (state[y] & 0xff)) & 0xff;
			result[i] = (byte) (buf[i] ^ state[xorIndex]);
		}
		return result;
	}

	public String encrypt(String input) {
		if (input == null)
			return null;
		try {
			return Hex.encodeHexString(rc4(URLEncoder.encode(input, "UTF-8").getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			e.printStackTrace();
			return input;
		}
	}

	public String decrypt(String input) {
		if (input == null)
			return null;
		try {
			return URLDecoder.decode(new String(rc4(Hex.decodeHex(input.toCharArray())), StandardCharsets.UTF_8),
					"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
			return input;
		}
	}

	public static RC4 getDefaultInstance() {
		SoftReference<RC4> instanceRef = pool.get();
		RC4 instance;
		if (instanceRef == null || (instance = instanceRef.get()) == null) {
			instance = new RC4();
			instanceRef = new SoftReference<>(instance);
			pool.set(instanceRef);
		}
		return instance;
	}

	public static String encryptWithKey(String str, String key) {
		RC4 rc4 = new RC4(key);
		return rc4.encrypt(str);
	}

	public static String decryptWithKey(String str, String key) {
		RC4 rc4 = new RC4(key);
		return rc4.decrypt(str);
	}

}
