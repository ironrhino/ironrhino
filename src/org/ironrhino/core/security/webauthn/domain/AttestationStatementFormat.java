package org.ironrhino.core.security.webauthn.domain;

import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

import org.ironrhino.core.security.webauthn.domain.cose.EC2Key;
import org.ironrhino.core.security.webauthn.domain.cose.Key;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.util.CodecUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AttestationStatementFormat {

	packed {
		@Override
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
					throw new IllegalArgumentException("Wrong alg");

				return AttestationType.Self;

			}

		}
	},

	@JsonProperty("fido-u2f")
	fido_u2f {
		@Override
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			// https://www.w3.org/TR/webauthn/#fido-u2f-attestation
			List<byte[]> x5c = attStmt.getX5c();
			if (x5c == null || x5c.size() != 1)
				throw new IllegalArgumentException("x5c should be only one");
			byte[] rpIdHash = authData.getRpIdHash();
			byte[] credentialId = authData.getAttestedCredential().getCredentialId();
			EC2Key credentialPublicKey = (EC2Key) authData.getAttestedCredential().getCredentialPublicKey();
			byte[] x = credentialPublicKey.getX();
			if (x.length != 32)
				throw new IllegalArgumentException("Wrong x coordinate: " + x.length);
			byte[] y = credentialPublicKey.getY();
			if (y.length != 32)
				throw new IllegalArgumentException("Wrong x coordinate: " + x.length);
			byte[] publicKeyU2F = Utils.concatByteArray(new byte[] { 0x04 }, x, y);
			byte[] verificationData = Utils.concatByteArray(new byte[] { 0x00 }, rpIdHash,
					CodecUtils.sha256(clientData.getRawData()), credentialId, publicKeyU2F);
			Signature verifier = Signature.getInstance(credentialPublicKey.getAlgorithm().getAlgorithmName());
			verifier.initVerify(Utils.generateCertificate(x5c.get(0)));
			verifier.update(verificationData);
			if (!verifier.verify(attStmt.getSig()))
				throw new IllegalArgumentException("Wrong signature");
			return AttestationType.Basic; // or AttestationType.AttCA
		}
	},
	none {
		@Override
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			return AttestationType.None;
		}
	},
	@JsonProperty("android-key")
	android_key {
		@Override
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			byte[] clientDataHash = CodecUtils.sha256(clientData.getRawData());
			byte[] verificationData = Utils.concatByteArray(authData.getRawData(), clientDataHash);
			Signature verifier = Signature.getInstance(attStmt.getAlg().getAlgorithmName());
			Certificate cert = Utils.generateCertificate(attStmt.getX5c().get(0));
			if (!cert.getPublicKey().equals(authData.getAttestedCredential().getCredentialPublicKey().getPublicKey()))
				throw new IllegalArgumentException("Public key not matched");
			verifier.initVerify(cert);
			verifier.update(verificationData);
			if (!verifier.verify(attStmt.getSig()))
				throw new IllegalArgumentException("Wrong signature");
			// TODO verify extension
			// byte[] attestationChallenge = (byte[])
			// authData.getExtensions().get("1.3.6.1.4.1.11129.2.1.17");
			// if (!Arrays.equals(attestationChallenge, clientDataHash))
			// throw new IllegalArgumentException("Wrong attestationChallenge");
			// return AttestationType.Basic;
			throw new UnsupportedOperationException("Not implemented for fmt: " + name());
		}
	},
	@JsonProperty("android-safetynet")
	android_safetynet {
		@Override
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {
			SafetyNetResponse response = Utils.JSON_OBJECTMAPPER.readValue(attStmt.getResponse(),
					SafetyNetResponse.class);
			byte[] clientDataHash = CodecUtils.sha256(clientData.getRawData());
			String nonce = Base64.getEncoder()
					.encodeToString(CodecUtils.sha256(Utils.concatByteArray(authData.getRawData(), clientDataHash)));
			if (!nonce.equals(response.getNonce()))
				throw new IllegalArgumentException("Wrong nonce");
			X509Certificate cert = Utils.generateCertificate(attStmt.getX5c().get(0));
			if (!cert.getSubjectX500Principal().getName().equals("attest.android.com"))
				throw new IllegalArgumentException("Wrong issuer of attestationCert");
			if (!response.isCtsProfileMatch())
				throw new IllegalArgumentException("Cts profile is not matched");
			return AttestationType.Basic;
		}
	},
	tpm {
		@Override
		public AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData, ClientData clientData)
				throws Exception {

			// return AttestationType.AttCA;
			throw new UnsupportedOperationException("Not implemented for fmt: " + name());
		}
	};

	public abstract AttestationType verify(AttestationStatement attStmt, AuthenticatorData authData,
			ClientData clientData) throws Exception;

}
