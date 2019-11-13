package org.ironrhino.core.security.webauthn.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PublicKeyCredentialOperationType {

	@JsonProperty("webauthn.create")
	CREATE, @JsonProperty("webauthn.get")
	GET;

}
