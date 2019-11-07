package org.ironrhino.core.security.webauthn.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuthenticatorAttestationResponse extends AuthenticatorResponse {

	private Attestation attestation;

	public void setAttestationObject(Attestation attestation) {
		this.attestation = attestation;
	}

}
