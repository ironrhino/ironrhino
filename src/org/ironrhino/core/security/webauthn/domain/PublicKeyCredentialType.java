package org.ironrhino.core.security.webauthn.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PublicKeyCredentialType {

	@JsonProperty("public-key")
	public_key;

}