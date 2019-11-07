package org.ironrhino.core.security.webauthn.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuthenticatorAssertionResponse extends AuthenticatorResponse {

	private AuthenticatorData authenticatorData;

	private byte[] signature;

	private byte[] userHandle;

}
