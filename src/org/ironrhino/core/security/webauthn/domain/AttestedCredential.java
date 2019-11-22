package org.ironrhino.core.security.webauthn.domain;

import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.internal.Utils;

import lombok.Data;

@Data
public class AttestedCredential {

	private final byte[] credentialId;

	private final byte[] aaguid;

	private final Key credentialPublicKey; // https://www.w3.org/TR/webauthn/#sctn-encoded-credPubKey-examples

	public static AttestedCredential valueOf(byte[] input) throws Exception {
		byte[] aaguid = new byte[16];
		System.arraycopy(input, 0, aaguid, 0, aaguid.length);

		byte[] length = new byte[2];
		System.arraycopy(input, 16, length, 0, length.length);
		int credentialIdLength = ((length[0] & 0xFF) << 8) | (length[1] & 0xFF);
		byte[] credentialId = new byte[credentialIdLength];
		System.arraycopy(input, 18, credentialId, 0, credentialId.length);

		byte[] rawCredentialPublicKey = new byte[input.length - 18 - credentialIdLength];
		System.arraycopy(input, 18 + credentialIdLength, rawCredentialPublicKey, 0, rawCredentialPublicKey.length);
		Key credentialPublicKey = Utils.CBOR_OBJECTMAPPER.readValue(rawCredentialPublicKey, Key.class);
		return new AttestedCredential(credentialId, aaguid, credentialPublicKey);

	}

}
