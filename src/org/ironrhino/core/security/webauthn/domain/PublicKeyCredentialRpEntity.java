package org.ironrhino.core.security.webauthn.domain;

import lombok.Value;

@Value
public class PublicKeyCredentialRpEntity {

	private String id;

	private String name;

	private String icon;

}