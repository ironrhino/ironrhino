package org.ironrhino.core.security.webauthn.domain.cose;

import com.fasterxml.jackson.annotation.JsonValue;

public enum KeyOperation {

	SIGN(1), VERIFY(2), ENCRYPT(3), DECRYPT(4), WRAP_KEY(5), UNWRAP_KEY(6), DERIVE_KEY(7), DERIVE_BITS(8),
	MAC_CREATE(9), MAC_VERIFY(10);

	private int value;

	KeyOperation(int value) {
		this.value = value;
	}

	@JsonValue
	public int toValue() {
		return value;
	}

	public static KeyOperation fromValue(int value) {
		for (KeyOperation e : values())
			if (e.value == value)
				return e;
		return null;
	}

}
