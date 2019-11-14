package org.ironrhino.core.security.webauthn.domain;

import lombok.Value;

@Value
public class PublicKeyCredentialUserEntity {

	private byte[] id;

	private String name;

	private String displayName;

	private String icon;

}