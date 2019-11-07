package org.ironrhino.core.security.webauthn.domain;

import static org.ironrhino.core.security.webauthn.internal.Utils.CBOR_OBJECTMAPPER;

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;

@Data
public class Attestation {

	private AttestationStatementFormat fmt;

	private AttestationStatement attStmt;

	private AuthenticatorData authData;

	@JsonCreator
	public static Attestation valueOf(String input) throws IOException {
		return CBOR_OBJECTMAPPER.readValue(Base64.getDecoder().decode(input), Attestation.class);
	}

}
