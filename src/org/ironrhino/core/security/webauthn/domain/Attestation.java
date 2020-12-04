package org.ironrhino.core.security.webauthn.domain;

import java.io.IOException;
import java.util.Base64;

import org.ironrhino.core.security.webauthn.internal.Utils;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor(onConstructor = @__(@JsonCreator))
public class Attestation {

	private AttestationStatementFormat fmt;

	private AttestationStatement attStmt;

	private AuthenticatorData authData;

	@JsonCreator
	public static Attestation valueOf(String input) throws IOException {
		return Utils.CBOR_OBJECTMAPPER.readValue(Base64.getDecoder().decode(input), Attestation.class);
	}

}
