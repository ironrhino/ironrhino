package org.ironrhino.core.security.util;

import java.io.File;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSA {
	private static Logger logger = LoggerFactory.getLogger(RSA.class);

	public static final String DEFAULT_KEY_LOCATION = "/resources/key/rsa";
	public static final String KEY_DIRECTORY = "/key/";
	// thread safe
	private static final ThreadLocal<SoftReference<RSA>> pool = new ThreadLocal<SoftReference<RSA>>() {
		@Override
		protected SoftReference<RSA> initialValue() {
			try {
				return new SoftReference<>(new RSA());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	};

	private static URI defaultKeystoreURI;
	private static String defaultPassword;

	static {
		File file = new File(AppInfo.getAppHome() + KEY_DIRECTORY + "rsa");
		if (file.exists()) {
			defaultKeystoreURI = file.toURI();
			logger.info("using file " + file.getAbsolutePath());
		} else {
			if (AppInfo.getStage() == Stage.PRODUCTION)
				logger.warn("file " + file.getAbsolutePath()
						+ " doesn't exists, please use your own keystore in production!");
			if (RSA.class.getResource(DEFAULT_KEY_LOCATION) != null) {
				try {
					defaultKeystoreURI = RSA.class.getResource(DEFAULT_KEY_LOCATION).toURI();
					logger.info("using classpath resource " + RSA.class.getResource(DEFAULT_KEY_LOCATION).toString()
							+ " as default keystore");
				} catch (URISyntaxException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		String s = System.getProperty(AppInfo.getAppName() + ".rsa.password");
		if (StringUtils.isNotBlank(s)) {
			defaultPassword = s;
			logger.info("using system property " + AppInfo.getAppName() + ".rc4 as default key");
		} else {
			try {
				file = new File(AppInfo.getAppHome() + KEY_DIRECTORY + "rsa.password");
				if (file.exists()) {
					defaultPassword = FileUtils.readFileToString(file, "UTF-8");
					logger.info("using file " + file.getAbsolutePath());
				} else {
					if (AppInfo.getStage() == Stage.PRODUCTION)
						logger.warn("file " + file.getAbsolutePath()
								+ " doesn't exists, please use your own default key in production!");
					if (RSA.class.getResource(DEFAULT_KEY_LOCATION) != null) {
						try (InputStream pis = RSA.class.getResourceAsStream(DEFAULT_KEY_LOCATION + ".password")) {
							defaultPassword = IOUtils.toString(pis, "UTF-8");
							logger.info("using classpath resource "
									+ RSA.class.getResource(DEFAULT_KEY_LOCATION + ".password").toString()
									+ " as default key");
						}
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}

	}

	private PrivateKey privateKey;
	private PublicKey publicKey;
	private X509Certificate certificate;

	public RSA() throws Exception {
		this(defaultKeystoreURI.toURL().openStream(), defaultPassword);
	}

	public RSA(InputStream is, String password) throws Exception {
		KeyStore ks = KeyStore.getInstance("pkcs12", "SunJSSE");
		ks.load(is, password.toCharArray());
		IOUtils.closeQuietly(is);
		Enumeration<String> aliases = ks.aliases();
		if (aliases.hasMoreElements()) {
			String alias = aliases.nextElement();
			privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
			Certificate[] cc = ks.getCertificateChain(alias);
			certificate = (X509Certificate) cc[0];
			publicKey = certificate.getPublicKey();
		}
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public byte[] encrypt(byte[] input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(input);
	}

	public byte[] decrypt(byte[] input) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(input);
	}

	public byte[] sign(byte[] input) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature sig = Signature.getInstance("SHA1WithRSA");
		sig.initSign(privateKey);
		sig.update(input);
		return sig.sign();
	}

	public boolean verify(byte[] input, byte[] signature)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature sig = Signature.getInstance("SHA1WithRSA");
		sig.initVerify(publicKey);
		sig.update(input);
		return sig.verify(signature);
	}

	public String encrypt(String str) {
		if (str == null)
			return null;
		try {
			return new String(Base64.getEncoder().encode(encrypt(str.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception ex) {
			logger.error("encrypt exception!", ex);
			return "";
		}
	}

	public String decrypt(String str) {
		if (str == null)
			return null;
		try {
			return new String(decrypt(Base64.getDecoder().decode(str.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception ex) {
			logger.error("decrypt exception!", ex);
			return "";
		}
	}

	public String sign(String str) {
		if (str == null)
			return null;
		try {
			return new String(Base64.getEncoder().encode(sign(str.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception ex) {
			logger.error("encrypt exception!", ex);
			return "";
		}
	}

	public boolean verify(String str, String signature) {
		if (str == null)
			return false;
		try {
			return verify(str.getBytes("UTF-8"), signature.getBytes("UTF-8"));
		} catch (Exception ex) {
			logger.error("encrypt exception!", ex);
			return false;
		}
	}

	public static RSA getDefaultInstance() {
		SoftReference<RSA> instanceRef = pool.get();
		RSA instance;
		if (instanceRef == null || (instance = instanceRef.get()) == null) {
			try {
				instance = new RSA();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			instanceRef = new SoftReference<>(instance);
			pool.set(instanceRef);
		}
		return instance;
	}

}
