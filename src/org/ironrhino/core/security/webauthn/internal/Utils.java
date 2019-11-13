package org.ironrhino.core.security.webauthn.internal;

import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.util.Base64;

import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Utils {

	public static final ObjectMapper JSON_OBJECTMAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	public static final ObjectMapper CBOR_OBJECTMAPPER = new ObjectMapper(new CBORFactory())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

	public static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

	public static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	public static final CertificateFactory X509_CERTIFICATE_FACTORY;
	static {
		try {
			X509_CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	};

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

}
