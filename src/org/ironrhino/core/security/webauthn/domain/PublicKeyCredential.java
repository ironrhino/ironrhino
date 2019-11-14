package org.ironrhino.core.security.webauthn.domain;

import lombok.Value;

@Value
public class PublicKeyCredential<R extends AuthenticatorResponse> {

	private byte[] id;

	private String type;

	private R response;

}
