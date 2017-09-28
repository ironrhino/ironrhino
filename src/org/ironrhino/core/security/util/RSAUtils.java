package org.ironrhino.core.security.util;

import java.io.ByteArrayOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/*
 * $ openssl 
 * OpenSSL> genrsa -out rsa_private_key.pem 1024 
 * OpenSSL> pkcs8 -topk8 -inform PEM -in rsa_private_key.pem -outform PEM -nocrypt -out rsa_private_key_pkcs8.pem // privateKey
 * OpenSSL> rsa -in rsa_private_key.pem -pubout -out rsa_public_key.pem // publicKey
 */
public class RSAUtils {

	public static final String KEY_ALGORTHM = "RSA";
	public static final String SPECIFIC_KEY_ALGORITHM = "RSA/ECB/PKCS1Padding";
	public static final String SIGNATURE_ALGORITHM = "SHA1WithRSA";
	public static final String CHARSET = "UTF-8";

	public static String encrypt(String data, String publicKey) throws Exception {
		return Base64.getEncoder().encodeToString(encryptByPublicKey(data.getBytes(CHARSET), publicKey));
	}

	public static String decrypt(String data, String privateKey) throws Exception {
		return new String(decryptByPrivateKey(Base64.getDecoder().decode(data), privateKey), CHARSET);
	}

	public static String sign(String data, String privateKey) throws Exception {
		return Base64.getEncoder().encodeToString(signWithPrivateKey(data.getBytes(CHARSET), privateKey));
	}

	public static boolean verify(String data, String publicKey, String sign) throws Exception {
		return verifyWithPublicKey(data.getBytes(CHARSET), publicKey, sign);
	}

	public static byte[] encryptByPublicKey(byte[] data, String key) throws Exception {
		byte[] encryptedData = null;
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(decodeKey(key));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		Key publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
		Cipher cipher = Cipher.getInstance(SPECIFIC_KEY_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		int maxEncryptBlockSize = getMaxEncryptBlockSize(keyFactory, publicKey);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			int dataLength = data.length;
			for (int i = 0; i < data.length; i += maxEncryptBlockSize) {
				int encryptLength = dataLength - i < maxEncryptBlockSize ? dataLength - i : maxEncryptBlockSize;
				byte[] doFinal = cipher.doFinal(data, i, encryptLength);
				bout.write(doFinal);
			}
			encryptedData = bout.toByteArray();
		} finally {
			if (bout != null) {
				bout.close();
			}
		}
		return encryptedData;
	}

	public static byte[] decryptByPrivateKey(byte[] data, String key) throws Exception {
		byte[] decryptedData = null;
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(decodeKey(key));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		Key privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		Cipher cipher = Cipher.getInstance(SPECIFIC_KEY_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		int maxDecryptBlockSize = getMaxDecryptBlockSize(keyFactory, privateKey);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			int dataLength = data.length;
			for (int i = 0; i < dataLength; i += maxDecryptBlockSize) {
				int decryptLength = dataLength - i < maxDecryptBlockSize ? dataLength - i : maxDecryptBlockSize;
				byte[] doFinal = cipher.doFinal(data, i, decryptLength);
				bout.write(doFinal);
			}
			decryptedData = bout.toByteArray();
		} finally {
			if (bout != null) {
				bout.close();
			}
		}
		return decryptedData;
	}

	public static byte[] signWithPrivateKey(byte[] data, String key) throws Exception {
		PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(decodeKey(key));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		PrivateKey privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(privateKey);
		signature.update(data);
		return signature.sign();
	}

	public static boolean verifyWithPublicKey(byte[] data, String key, String sign) throws Exception {
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(decodeKey(key));
		KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORTHM);
		PublicKey publickey = keyFactory.generatePublic(x509EncodedKeySpec);
		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initVerify(publickey);
		signature.update(data);
		return signature.verify(Base64.getDecoder().decode(sign));
	}

	private static int getMaxEncryptBlockSize(KeyFactory keyFactory, Key key) throws Exception {
		int maxLength = 117;
		try {
			RSAPublicKeySpec publicKeySpec = keyFactory.getKeySpec(key, RSAPublicKeySpec.class);
			int keyLength = publicKeySpec.getModulus().bitLength();
			maxLength = keyLength / 8 - 11;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return maxLength;
	}

	private static int getMaxDecryptBlockSize(KeyFactory keyFactory, Key key) throws Exception {
		int maxLength = 128;
		try {
			RSAPrivateKeySpec publicKeySpec = keyFactory.getKeySpec(key, RSAPrivateKeySpec.class);
			int keyLength = publicKeySpec.getModulus().bitLength();
			maxLength = keyLength / 8;
		} catch (Exception e) {
		}
		return maxLength;
	}

	private static byte[] decodeKey(String key) {
		String delimiter = "-----";
		if (key.indexOf(delimiter) >= 0) {
			int start = key.indexOf(delimiter, key.indexOf(delimiter) + delimiter.length()) + delimiter.length() + 1;
			int end = key.indexOf(delimiter, start) - 1;
			key = key.substring(start, end);
		}
		key = key.replaceAll("\\s", "");
		return Base64.getDecoder().decode(key);
	}

}
