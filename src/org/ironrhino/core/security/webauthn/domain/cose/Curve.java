package org.ironrhino.core.security.webauthn.domain.cose;

import java.security.AlgorithmParameters;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

public enum Curve {

	secp256r1(1), secp384r1(2), secp521r1(3);

	private final int value;

	@Getter
	private final ECParameterSpec parameterSpec;

	private Curve(int value) {
		this.value = value;
		this.parameterSpec = createECParameterSpec(name());
	}

	@JsonValue
	public int toValue() {
		return value;
	}

	@JsonCreator
	public static Curve fromValue(int value) {
		for (Curve e : values())
			if (e.value == value)
				return e;
		return null;
	}

	private static ECParameterSpec createECParameterSpec(String name) {
		try {
			AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
			parameters.init(new ECGenParameterSpec(name));
			return parameters.getParameterSpec(ECParameterSpec.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
