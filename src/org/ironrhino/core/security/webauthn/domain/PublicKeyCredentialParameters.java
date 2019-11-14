package org.ironrhino.core.security.webauthn.domain;

import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PublicKeyCredentialParameters {

	private PublicKeyCredentialType type;

	private Algorithm alg;

	public PublicKeyCredentialParameters() {
		this.type = PublicKeyCredentialType.public_key;
		this.alg = Algorithm.ES256;
	}

}