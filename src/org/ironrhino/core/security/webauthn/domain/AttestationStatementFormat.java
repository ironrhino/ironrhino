package org.ironrhino.core.security.webauthn.domain;

import java.security.Signature;
import java.util.List;

import org.ironrhino.core.security.webauthn.domain.cose.EC2Key;
import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AttestationStatementFormat {

	packed {
		@SuppressWarnings("unused")
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			// TODO not tested
			if (true)
				throw new UnsupportedOperationException("Not implemented for fmt: " + name());

			byte[] verificationData = Utils.concatByteArray(authData.getRawData(),
					CodecUtils.sha256(clientData.getRawData()));

			if (attStmt.getEcdaaKeyId() != null) {
				// ECDAA

				return AttestationType.ECDAA;
			} else if (attStmt.getX5c() != null) {
				// X.509

				return AttestationType.Basic; // or AttestationType.AttCA
			} else {
				// self attestation
				Key credentialPublicKey = authData.getAttestedCredential().getCredentialPublicKey();

				if (attStmt.getAlg() == null || attStmt.getAlg() != credentialPublicKey.getAlgorithm())
					throw new RuntimeException("Wrong alg");

				return AttestationType.Self;

			}

		}
	},

	@JsonProperty("fido-u2f")
	fido_u2f {
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			// https://www.w3.org/TR/webauthn/#fido-u2f-attestation
			List<byte[]> x5c = attStmt.getX5c();
			if (x5c == null || x5c.size() != 1)
				throw new RuntimeException("x5c should be only one");
			byte[] rpIdHash = authData.getRpIdHash();
			byte[] credentialId = authData.getAttestedCredential().getCredentialId();
			EC2Key credentialPublicKey = (EC2Key) authData.getAttestedCredential().getCredentialPublicKey();
			byte[] x = (byte[]) credentialPublicKey.getX();
			if (x.length != 32)
				throw new RuntimeException("Wrong x coordinate: " + x.length);
			byte[] y = (byte[]) credentialPublicKey.getY();
			if (y.length != 32)
				throw new RuntimeException("Wrong x coordinate: " + x.length);
			byte[] publicKeyU2F = Utils.concatByteArray(new byte[] { 0x04 }, x, y);
			byte[] verificationData = Utils.concatByteArray(new byte[] { 0x00 }, rpIdHash,
					CodecUtils.sha256(clientData.getRawData()), credentialId, publicKeyU2F);
			Signature verifier = Signature.getInstance(credentialPublicKey.getAlgorithm().getAlgorithmName());
			verifier.initVerify(Utils.generateCertificate(x5c.get(0)));
			verifier.update(verificationData);
			if (!verifier.verify(attStmt.getSig()))
				throw new RuntimeException("Wrong signature");
			return AttestationType.Basic; // or AttestationType.AttCA
		}
	},
	none {
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			return AttestationType.None;
		}
	},
	@JsonProperty("android-key")
	android_key {
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			// return AttestationType.Basic;
			throw new UnsupportedOperationException("Not implemented for fmt: " + name());
		}
	},
	@JsonProperty("android-safetynet")
	android_safetynet {
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			// return AttestationType.Basic;
			throw new UnsupportedOperationException("Not implemented for fmt: " + name());
		}
	},
	tpm {
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {

			// return AttestationType.AttCA;
			throw new UnsupportedOperationException("Not implemented for fmt: " + name());
		}
	};

	public abstract AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData,
			ClientData clientData) throws Exception;

}
