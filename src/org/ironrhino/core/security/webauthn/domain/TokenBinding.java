package org.ironrhino.core.security.webauthn.domain;

import lombok.Value;

@Value
public class TokenBinding {

	private String id;

	private TokenBindingStatus status;

}
