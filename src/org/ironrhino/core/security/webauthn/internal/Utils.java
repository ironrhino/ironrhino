package org.ironrhino.core.security.webauthn.internal;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

	public static final ObjectMapper JSON_OBJECTMAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setSerializationInclusion(JsonInclude.Include.NON_NULL).registerModule(new ParameterNamesModule());

	public static final ObjectMapper CBOR_OBJECTMAPPER = new ObjectMapper(new CBORFactory())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(new ParameterNamesModule());

	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private static final CertificateFactory X509_CERTIFICATE_FACTORY;
	static {
		try {
			X509_CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	};

	public static String encodeBase64url(byte[] input) {
		return BASE64_URL_ENCODER.encodeToString(input);
	}

	public static byte[] decodeBase64url(String input) {
		return BASE64_URL_DECODER.decode(input);
	}

	public static String generateChallenge(int length) {
		return BASE64_URL_ENCODER.encodeToString(CodecUtils.nextId(length).getBytes(StandardCharsets.UTF_8));
	}

	public static byte[] concatByteArray(byte[]... arrays) {
		int totalLength = 0;
		for (int i = 0; i < arrays.length; i++) {
			totalLength += arrays[i].length;
		}
		byte[] result = new byte[totalLength];
		int currentIndex = 0;
		for (int i = 0; i < arrays.length; i++) {
			System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
			currentIndex += arrays[i].length;
		}
		return result;
	}

	public static X509Certificate generateCertificate(byte[] input) throws CertificateException {
		return (X509Certificate) X509_CERTIFICATE_FACTORY.generateCertificate(new ByteArrayInputStream(input));
	}

}
