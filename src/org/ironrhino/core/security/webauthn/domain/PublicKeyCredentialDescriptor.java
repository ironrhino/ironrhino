package org.ironrhino.core.security.webauthn.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PublicKeyCredentialDescriptor {

	private byte[] id;

	private PublicKeyCredentialType type = PublicKeyCredentialType.public_key;

	private List<AuthenticatorTransport> transports;

	public PublicKeyCredentialDescriptor(byte[] id) {
		this.id = id;
		this.transports = null;
	}

}