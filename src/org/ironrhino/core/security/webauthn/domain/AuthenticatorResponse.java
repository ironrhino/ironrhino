package org.ironrhino.core.security.webauthn.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticatorResponse {

	@JsonProperty("clientDataJSON")
	private final ClientData clientData;

}
