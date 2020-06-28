package org.ironrhino.core.security.jwt;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import lombok.Data;

public class Jwt {

	private static final String ALGORITHM = "HmacSHA256";

	private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

	private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

	private static final String header = BASE64_URL_ENCODER
			.encodeToString("{\"alg\": \"HS256\", \"typ\": \"JWT\"}".getBytes());

	public static final ObjectMapper JSON_OBJECTMAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setSerializationInclusion(JsonInclude.Include.NON_NULL).registerModule(new ParameterNamesModule());

	public static String createWithSubject(String subject, Duration lifetime, String secret) {
		try {
			LocalDateTime now = LocalDateTime.now();
			Payload pl = new Payload();
			pl.setSub(subject);
			pl.setIat(now.atZone(ZoneId.systemDefault()).toEpochSecond());
			if (lifetime != null) {
				pl.setExp(now.plus(lifetime).atZone(ZoneId.systemDefault()).toEpochSecond());
			}
			String payload = BASE64_URL_ENCODER.encodeToString(JSON_OBJECTMAPPER.writeValueAsBytes(pl));
			String signature = digestWithHmacSHA256(header + '.' + payload, secret);
			return header + '.' + payload + '.' + signature;
		} catch (JsonProcessingException e) {
			// Should never throw
			throw new RuntimeException(e);
		}
	}

	public static String extractSubject(String jwt) {
		String[] arr = jwt.split("\\.");
		if (arr.length != 3)
			throw new IllegalArgumentException("Invalid JWT: " + jwt);
		try {
			Payload payload = JSON_OBJECTMAPPER.readValue(BASE64_URL_DECODER.decode(arr[1]), Payload.class);
			if (payload.getExp() > 0
					&& payload.getExp() + 60 < LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond())
				throw new IllegalArgumentException("Expired JWT: " + jwt);
			return payload.getSub();
		} catch (IOException e) {
			throw new IllegalArgumentException("Invalid payload: " + jwt);
		}
	}

	public static void verifySignature(String jwt, String secret) {
		String[] arr = jwt.split("\\.");
		if (!digestWithHmacSHA256(jwt.substring(0, jwt.lastIndexOf('.')), secret).equals(arr[2]))
			throw new IllegalArgumentException("Invalid signature: " + jwt);
	}

	private static String digestWithHmacSHA256(String input, String secret) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(), ALGORITHM));
			return BASE64_URL_ENCODER.encodeToString(mac.doFinal(input.getBytes()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Data
	static class Payload {
		private String sub;
		private long iat;
		private long exp;
	}

}
