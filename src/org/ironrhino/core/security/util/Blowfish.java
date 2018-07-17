package org.ironrhino.core.security.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.CodecUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Blowfish {

	public static final String DEFAULT_KEY_LOCATION = "/resources/key/blowfish";
	public static final String KEY_DIRECTORY = "/key/";

	private static String CIPHER_NAME = "Blowfish/CFB8/NoPadding";
	private static String KEY_SPEC_NAME = "Blowfish";
	public static int KEY_LENGTH = 16;
	// thread safe
	private static final ThreadLocal<SoftReference<Blowfish>> pool = new ThreadLocal<SoftReference<Blowfish>>() {
		@Override
		protected SoftReference<Blowfish> initialValue() {
			return new SoftReference<>(new Blowfish());
		}
	};

	private static String defaultKey;
	private Cipher enCipher;
	private Cipher deCipher;

	static {
		String s = System.getProperty(AppInfo.getAppName() + ".blowfish");
		if (StringUtils.isNotBlank(s)) {
			defaultKey = s;
			log.info("using system property " + AppInfo.getAppName() + ".blowfish as default key");
		} else {
			try {
				File file = new File(AppInfo.getAppHome() + KEY_DIRECTORY + "blowfish");
				if (file.exists()) {
					try (BufferedReader br = new BufferedReader(
							new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
						defaultKey = br.lines().collect(Collectors.joining("\n"));
						log.info("using file " + file.getAbsolutePath());
					}
				} else {
					if (AppInfo.getStage() == Stage.PRODUCTION)
						log.error("file " + file.getAbsolutePath()
								+ " doesn't exists, please use your own default key in production!");
					if (Blowfish.class.getResource(DEFAULT_KEY_LOCATION) != null) {
						try (BufferedReader br = new BufferedReader(new InputStreamReader(
								Blowfish.class.getResourceAsStream(DEFAULT_KEY_LOCATION), StandardCharsets.UTF_8))) {
							defaultKey = br.lines().collect(Collectors.joining("\n"));
							log.info("using classpath resource "
									+ Blowfish.class.getResource(DEFAULT_KEY_LOCATION).toString() + " as default key");
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		if (defaultKey == null)
			defaultKey = AppInfo.getAppName() + ':' + AppInfo.getAppBasePackage();
		defaultKey = CodecUtils.fuzzify(defaultKey);
	}

	public Blowfish() {
		this(defaultKey);
	}

	public Blowfish(String key) {
		key = DigestUtils.md5Hex(key).substring(0, KEY_LENGTH);
		try {
			SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), KEY_SPEC_NAME);
			IvParameterSpec ivParameterSpec = new IvParameterSpec((key.substring(0, 8)).getBytes());
			enCipher = Cipher.getInstance(CIPHER_NAME);
			deCipher = Cipher.getInstance(CIPHER_NAME);
			enCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
			deCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
		} catch (Exception e) {
			log.error("[BlowfishEncrypter]", e);
		}
	}

	public byte[] encrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
		return enCipher.doFinal(bytes);
	}

	public byte[] encrypt(byte[] bytes, int offset, int length) throws IllegalBlockSizeException, BadPaddingException {
		return enCipher.doFinal(bytes, offset, length);
	}

	public byte[] decrypt(byte[] bytes) throws IllegalBlockSizeException, BadPaddingException {
		return deCipher.doFinal(bytes);
	}

	public byte[] decrypt(byte[] bytes, int offset, int length) throws IllegalBlockSizeException, BadPaddingException {
		return deCipher.doFinal(bytes, offset, length);
	}

	public String encrypt(String str) {
		if (str == null)
			return null;
		try {
			return new String(
					Base64.getEncoder().withoutPadding().encode(encrypt(str.getBytes(StandardCharsets.UTF_8))),
					StandardCharsets.UTF_8);
		} catch (Exception ex) {
			log.error("encrypt exception!", ex);
			return "";
		}
	}

	public String decrypt(String str) {
		if (str == null)
			return null;
		try {
			return new String(decrypt(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8))),
					StandardCharsets.UTF_8);
		} catch (Exception ex) {
			log.error("decrypt exception!", ex);
			return "";
		}
	}

	public static Blowfish getDefaultInstance() {
		SoftReference<Blowfish> instanceRef = pool.get();
		Blowfish instance;
		if (instanceRef == null || (instance = instanceRef.get()) == null) {
			instance = new Blowfish();
			instanceRef = new SoftReference<>(instance);
			pool.set(instanceRef);
		}
		return instance;
	}

	public static String encryptWithSalt(String str, String salt) {
		Blowfish blowfish = new Blowfish(defaultKey + salt);
		try {
			return new String(
					Base64.getEncoder().withoutPadding().encode(blowfish.encrypt(str.getBytes(StandardCharsets.UTF_8))),
					StandardCharsets.UTF_8);
		} catch (Exception ex) {
			log.error("encrypt exception!", ex);
			return "";
		}
	}

	public static String decryptWithSalt(String str, String salt) {
		Blowfish blowfish = new Blowfish(defaultKey + salt);
		try {
			return new String(blowfish.decrypt(Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8))),
					StandardCharsets.UTF_8);
		} catch (Exception ex) {
			log.error("decrypt exception!", ex);
			return "";
		}
	}

}
