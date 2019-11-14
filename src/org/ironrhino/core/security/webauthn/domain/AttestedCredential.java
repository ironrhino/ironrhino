package org.ironrhino.core.security.webauthn.domain;

import java.security.Signature;

import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Data;

@Data
public class AttestedCredential {

	private final byte[] aaguid;

	private final byte[] credentialId;

	private final Key credentialPublicKey; // https://www.w3.org/TR/webauthn/#sctn-encoded-credPubKey-examples

	@JsonCreator
	public AttestedCredential(byte[] input) throws Exception {
		aaguid = new byte[16];
		System.arraycopy(input, 0, aaguid, 0, aaguid.length);

		byte[] length = new byte[2];
		System.arraycopy(input, 16, length, 0, length.length);
		int credentialIdLength = (int) (((length[0] & 0xFF) << 8) | (length[1] & 0xFF));
		credentialId = new byte[credentialIdLength];
		System.arraycopy(input, 18, credentialId, 0, credentialId.length);

		byte[] rawCredentialPublicKey = new byte[input.length - 18 - credentialIdLength];
		System.arraycopy(input, 18 + credentialIdLength, rawCredentialPublicKey, 0, rawCredentialPublicKey.length);
		credentialPublicKey = Utils.CBOR_OBJECTMAPPER.readValue(rawCredentialPublicKey, Key.class);

	}

	public void verifySignature(AuthenticatorData authData, ClientData clientData, byte[] signature) throws Exception {
		byte[] verificationData = Utils.concatByteArray(authData.getRawData(),
				CodecUtils.sha256(clientData.getRawData()));
		Signature verifier = Signature.getInstance(credentialPublicKey.getAlgorithm().getAlgorithmName());
		verifier.initVerify(credentialPublicKey.getPublicKey());
		verifier.update(verificationData);
		if (!verifier.verify(signature))
			throw new IllegalArgumentException("Wrong signature");
	}

}
