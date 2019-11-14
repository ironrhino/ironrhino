package org.ironrhino.core.security.webauthn.domain;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class AuthenticatorAssertionResponse extends AuthenticatorResponse {

	private final AuthenticatorData authenticatorData;

	private final byte[] signature;

	private final byte[] userHandle;

	public AuthenticatorAssertionResponse(ClientData clientData, AuthenticatorData authenticatorData, byte[] signature,
			byte[] userHandle) {
		super(clientData);
		this.authenticatorData = authenticatorData;
		this.signature = signature;
		this.userHandle = userHandle;
	}

}
