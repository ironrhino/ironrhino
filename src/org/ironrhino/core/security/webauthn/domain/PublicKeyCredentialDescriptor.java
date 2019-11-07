package org.ironrhino.core.security.webauthn.domain;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PublicKeyCredentialDescriptor {

	private byte[] id;

	private PublicKeyCredentialType type = PublicKeyCredentialType.public_key;

	private List<AuthenticatorTransport> transports;

	public PublicKeyCredentialDescriptor(byte[] id) {
		this.id = id;
	}

}