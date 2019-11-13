package org.ironrhino.core.security.webauthn.domain.cose;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

//https://tools.ietf.org/html/rfc8152#section-13
public enum KeyType {

	OKP(1), EC2(2), RSA(3), SYMMETRIC(4), RESERVED(0);

	private final int value;

	private KeyType(int value) {
		this.value = value;
	}

	@JsonValue
	public int toValue() {
		return value;
	}

	@JsonCreator
	public static KeyType fromValue(int value) {
		for (KeyType e : values())
			if (e.value == value)
				return e;
		return null;
	}

}
