package org.ironrhino.core.security.webauthn.domain;

import lombok.Data;

@Data
public class TokenBinding {

	private String id;

	private TokenBindingStatus status;

}
