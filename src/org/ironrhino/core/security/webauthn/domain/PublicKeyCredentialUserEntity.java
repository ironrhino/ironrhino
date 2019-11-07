package org.ironrhino.core.security.webauthn.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyCredentialUserEntity {

	private byte[] id;

	private String name;

	private String displayName;

	private String icon;

}