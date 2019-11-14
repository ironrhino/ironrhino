package org.ironrhino.core.security.webauthn.domain;

import java.security.Signature;

import org.ironrhino.core.security.webauthn.InvalidSignCountException;
import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.util.CodecUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoredCredential extends AttestedCredential {

	private final String username;

	private int signCount;

	public StoredCredential(byte[] aaguid, byte[] credentialId, Key credentialPublicKey, String username,
			int signCount) {
		super(aaguid, credentialId, credentialPublicKey);
		this.username = username;
		this.signCount = signCount;
	}

	public void verifySignature(AuthenticatorData authData, ClientData clientData, byte[] signature) throws Exception {
		if ((authData.getSignCount() > 0 || signCount > 0) && authData.getSignCount() <= signCount)
			throw new InvalidSignCountException(
					"The authenticator may be cloned, signCount should great than " + signCount);
		byte[] verificationData = Utils.concatByteArray(authData.getRawData(),
				CodecUtils.sha256(clientData.getRawData()));
		Key credentialPublicKey = getCredentialPublicKey();
		Signature verifier = Signature.getInstance(credentialPublicKey.getAlgorithm().getAlgorithmName());
		verifier.initVerify(credentialPublicKey.getPublicKey());
		verifier.update(verificationData);
		if (!verifier.verify(signature))
			throw new IllegalArgumentException("Wrong signature");
	}

}
