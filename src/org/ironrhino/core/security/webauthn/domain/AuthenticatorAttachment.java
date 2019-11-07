package org.ironrhino.core.security.webauthn.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AuthenticatorAttachment {

	platform, @JsonProperty("cross-platform")
	cross_platform;

}