package org.ironrhino.core.security.webauthn.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class AuthenticatorResponse {

	@JsonProperty("clientDataJSON")
	private ClientData clientData;

}
