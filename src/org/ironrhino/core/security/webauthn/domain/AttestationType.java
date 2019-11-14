package org.ironrhino.core.security.webauthn.domain;

import lombok.Getter;

public enum AttestationType {

	Basic("Basic Attestation"), Self("Self Attestation"), AttCA("Attestation CA"),
	ECDAA("Elliptic Curve based Direct Anonymous Attestation"), None("No attestation statement");

	@Getter
	private final String description;

	private AttestationType(String description) {
		this.description = description;
	}

}
