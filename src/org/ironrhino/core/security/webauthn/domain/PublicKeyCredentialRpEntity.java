package org.ironrhino.core.security.webauthn.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyCredentialRpEntity {

	private String id;

	private String name;

	private String icon;

}