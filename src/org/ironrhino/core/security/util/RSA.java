package org.ironrhino.core.security.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSA {
	private static Logger log = LoggerFactory.getLogger(RSA.class);

	public static final String DEFAULT_KEY_LOCATION = "/resources/key/rsa";
	public static final String KEY_DIRECTORY = "/key/";
	private static RSA rsa;

	static {
		InputStream is = null;
		String password = null;
		File file = new File(AppInfo.getAppHome() + KEY_DIRECTORY + "rsa");
		if (file.exists()) {
			try {
				is = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				log.error(e.getMessage(), e);
			}
			log.info("using file " + file.getAbsolutePath());
		} else {
			if (AppInfo.getStage() == Stage.PRODUCTION)
				log.warn("file "
						+ file.getAbsolutePath()
						+ " doesn't exists, please use your own keystore in production!");
			if (RSA.class.getResource(DEFAULT_KEY_LOCATION) != null) {
				is = RSA.class.getResourceAsStream(DEFAULT_KEY_LOCATION);
				log.info("using classpath resource "
						+ RSA.class.getResource(DEFAULT_KEY_LOCATION)
								.toString() + " as default keystore");
			}
		}
		String s = System.getProperty(AppInfo.getAppName() + ".rsa.password");
		if (StringUtils.isNotBlank(s)) {
			password = s;
			log.info("using system property " + AppInfo.getAppName()
					+ ".rc4 as default key");
		} else {
			try {
				file = new File(AppInfo.getAppHome() + KEY_DIRECTORY
						+ "rsa.password");
				if (file.exists()) {
					password = FileUtils.readFileToString(file, "UTF-8");
					log.info("using file " + file.getAbsolutePath());
				} else {
					if (AppInfo.getStage() == Stage.PRODUCTION)
						log.warn("file "
								+ file.getAbsolutePath()
								+ " doesn't exists, please use your own default key in production!");
					if (RSA.class.getResource(DEFAULT_KEY_LOCATION) != null) {
						password = IOUtils.toString(RSA.class
								.getResourceAsStream(DEFAULT_KEY_LOCATION
										+ ".password"), "UTF-8");
						log.info("using classpath resource "
								+ RSA.class.getResource(
										DEFAULT_KEY_LOCATION + ".password")
										.toString() + " as default key");
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		if (is != null && password != null)
			try {
				rsa = new RSA(is, password);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
	}

	private PrivateKey privateKey;
	private PublicKey publicKey;
	private X509Certificate certificate;

	public RSA(InputStream is, String password) throws KeyStoreException,
			NoSuchProviderException, NoSuchAlgorithmException,
			CertificateException, IOException, UnrecoverableKeyException {
		KeyStore ks = KeyStore.getInstance("pkcs12", "SunJSSE");
		ks.load(is, password.toCharArray());
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

	public byte[] encrypt(byte[] input) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(input);
	}

	public byte[] decrypt(byte[] input) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(input);
	}

	public byte[] sign(byte[] input) throws NoSuchAlgorithmException,
			InvalidKeyException, SignatureException {
		Signature sig = Signature.getInstance("SHA1WithRSA");
		sig.initSign(privateKey);
		sig.update(input);
		return sig.sign();
	}

	public boolean verify(byte[] input, byte[] signature)
			throws NoSuchAlgorithmException, InvalidKeyException,
			SignatureException {
		Signature sig = Signature.getInstance("SHA1WithRSA");
		sig.initVerify(publicKey);
		sig.update(input);
		return sig.verify(signature);
	}

	public static String encrypt(String str) {
		if (str == null)
			return null;
		try {
			return new String(Base64.encodeBase64(rsa.encrypt(str
					.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception ex) {
			log.error("encrypt exception!", ex);
			return "";
		}
	}

	public static String decrypt(String str) {
		if (str == null)
			return null;
		try {
			return new String(rsa.decrypt(Base64.decodeBase64(str
					.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception ex) {
			log.error("decrypt exception!", ex);
			return "";
		}
	}

	public static String sign(String str) {
		if (str == null)
			return null;
		try {
			return new String(Base64.encodeBase64(rsa.sign(str
					.getBytes("UTF-8"))), "UTF-8");
		} catch (Exception ex) {
			log.error("encrypt exception!", ex);
			return "";
		}
	}

	public static boolean verify(String str, String signature) {
		if (str == null)
			return false;
		try {
			return rsa.verify(str.getBytes("UTF-8"),
					signature.getBytes("UTF-8"));
		} catch (Exception ex) {
			log.error("encrypt exception!", ex);
			return false;
		}
	}

}
