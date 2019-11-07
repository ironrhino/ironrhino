package org.ironrhino.core.security.webauthn.domain.cose;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

public enum Algorithm {

	ES256(-7, "SHA256withECDSA"), EdDSA(-8, "EDDSA"), RS256(-257, "SHA256withRSA"), RS1(-65535, "SHA1withRSA");

	private final int value;

	@Getter
	private final String algorithmName;

	private Algorithm(int value, String name) {
		this.value = value;
		this.algorithmName = name;
	}

	@JsonValue
	public int toValue() {
		return value;
	}

	@JsonCreator
	public static Algorithm fromValue(int value) {
		for (Algorithm e : values())
			if (e.value == value)
				return e;
		return null;
	}

}
