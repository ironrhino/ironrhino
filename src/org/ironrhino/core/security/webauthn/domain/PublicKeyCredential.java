package org.ironrhino.core.security.webauthn.domain;

import lombok.Data;

@Data
public class PublicKeyCredential<R extends AuthenticatorResponse, E extends ClientExtensionOutputs> {

	private String id;

	private String type;

	private R response;

	private E clientExtensionResults;

}
