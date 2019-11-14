package org.ironrhino.core.security.webauthn.domain;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class AuthenticatorAttestationResponse extends AuthenticatorResponse {

	private final Attestation attestationObject;

	public AuthenticatorAttestationResponse(ClientData clientData, Attestation attestationObject) {
		super(clientData);
		this.attestationObject = attestationObject;
	}

}
