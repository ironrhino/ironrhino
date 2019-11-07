package org.ironrhino.core.security.webauthn.domain;

import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyCredentialParameters {

	private PublicKeyCredentialType type = PublicKeyCredentialType.public_key;

	private Algorithm alg = Algorithm.ES256;

}