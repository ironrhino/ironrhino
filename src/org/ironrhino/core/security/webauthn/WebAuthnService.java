package org.ironrhino.core.security.webauthn;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.ironrhino.core.security.webauthn.domain.Attestation;
import org.ironrhino.core.security.webauthn.domain.AttestationConveyancePreference;
import org.ironrhino.core.security.webauthn.domain.AttestationStatement;
import org.ironrhino.core.security.webauthn.domain.AttestationType;
import org.ironrhino.core.security.webauthn.domain.AttestedCredentialData;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAssertionResponse;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAttachment;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAttestationResponse;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorData;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorSelectionCriteria;
import org.ironrhino.core.security.webauthn.domain.ClientData;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialCreationOptions;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialDescriptor;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialOperationType;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialParameters;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialRequestOptions;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialRpEntity;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialType;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialUserEntity;
import org.ironrhino.core.security.webauthn.domain.UserVerificationRequirement;
import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.servlet.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnService {

	@Value("${webAuthn.rp.id:localhost}")
	private String rpId;

	@Value("${webAuthn.rp.name:Localhost}")
	private String rpName;

	@Value("${webAuthn.rp.icon:#{null}}")
	private String rpIcon;

	@Value("${webAuthn.challenge.length:32}")
	private int challengeLength = 32;

	@Value("${webAuthn.timeout:60000}")
	private long timeout = 60000L;

	@Value("${webAuthn.authenticatorSelection.authenticatorAttachment:cross_platform}")
	private AuthenticatorAttachment authenticatorAttachment;

	@Value("${webAuthn.authenticatorSelection.requireResidentKey:false}")
	private boolean requireResidentKey;

	@Value("${webAuthn.authenticatorSelection.userVerification:preferred}")
	private UserVerificationRequirement userVerification;

	@Value("${webAuthn.attestation:direct}")
	private AttestationConveyancePreference attestation;

	@Autowired
	private WebAuthnCredentialService credentialService;

	public PublicKeyCredentialCreationOptions buildCreationOptions(String id, String username, String name) {
		PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions();

		PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(
				RequestContext.getRequest() != null ? RequestContext.getRequest().getServerName() : rpId, rpName,
				rpIcon);
		options.setRp(rp);

		PublicKeyCredentialUserEntity user = new PublicKeyCredentialUserEntity(id.getBytes(StandardCharsets.UTF_8),
				username, name, null);
		options.setUser(user);

		String challenge = Utils.generateChallenge(challengeLength);
		credentialService.putChallenge(username, challenge, (int) timeout);
		options.setChallenge(challenge);

		options.setPubKeyCredParams(
				Arrays.asList(new PublicKeyCredentialParameters(PublicKeyCredentialType.public_key, Algorithm.ES256)));

		options.setTimeout(timeout);

		options.setAuthenticatorSelection(
				new AuthenticatorSelectionCriteria(authenticatorAttachment, requireResidentKey, userVerification));

		options.setAttestation(attestation);

		options.setExcludeCredentials(credentialService.getCredentials(username).stream()
				.map(cridential -> new PublicKeyCredentialDescriptor(cridential.getCredentialId()))
				.collect(Collectors.toList()));

		return options;
	}

	public void verifyAttestation(PublicKeyCredential<AuthenticatorAttestationResponse> credential, String username)
			throws Exception {
		// https://www.w3.org/TR/webauthn/#registering-a-new-credential
		ClientData clientData = credential.getResponse().getClientData();
		String challenge = credentialService.getChallenge(username);
		clientData.verify(PublicKeyCredentialOperationType.CREATE, rpId, challenge);

		Attestation attestation = credential.getResponse().getAttestation();

		AuthenticatorData authData = attestation.getAuthData();
		authData.verify(rpId, userVerification);

		AttestationStatement attStmt = attestation.getAttStmt();
		AttestationType attestationType = attestation.getFmt().verify(attStmt, authData, clientData);
		assessTrustworthiness(attestation, attestationType);

		credentialService.addCredentials(username, authData.getAttestedCredentialData());

	}

	protected void assessTrustworthiness(Attestation attestation, AttestationType attestationType) {
		// step 16 check policy
		switch (attestationType) {
		case Basic:
			break;
		case Self:
			break;
		case AttCA:
			break;
		case ECDAA:
			break;
		default:
			throw new RuntimeException("Untrusted AttestationType: " + attestationType);
		}
	}

	public PublicKeyCredentialRequestOptions buildRequestOptions(String username) {
		PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions();
		String challenge = Utils.generateChallenge(challengeLength);
		credentialService.putChallenge(username, challenge, (int) timeout);
		options.setChallenge(challenge);
		options.setTimeout(timeout);
		options.setRpId(rpId);
		options.setUserVerification(userVerification);
		options.setAllowCredentials(credentialService.getCredentials(username).stream()
				.map(cridential -> new PublicKeyCredentialDescriptor(cridential.getCredentialId()))
				.collect(Collectors.toList()));
		return options;
	}

	public void verifyAssertion(PublicKeyCredential<AuthenticatorAssertionResponse> credential, String username)
			throws Exception {
		// https://www.w3.org/TR/webauthn/#verifying-assertion

		byte[] userHandle = credential.getResponse().getUserHandle();
		if (userHandle != null) {
			String user = new String(userHandle); // TODO id -> username;
			if (username == null) {
				username = user;
			} else if (!username.equals(user)) {
				throw new RuntimeException("userHandle not matches");
			}
		} else if (username == null) {
			throw new IllegalArgumentException("username should be present");
		}

		Optional<AttestedCredentialData> publicKey = credentialService.getCredentials(username).stream()
				.filter(pk -> Arrays.equals(pk.getCredentialId(), Utils.decodeBase64url(credential.getId()))).findAny();
		if (!publicKey.isPresent())
			throw new RuntimeException("Unregistered credential");

		ClientData clientData = credential.getResponse().getClientData();
		String challenge = credentialService.getChallenge(username);
		clientData.verify(PublicKeyCredentialOperationType.GET, rpId, challenge);

		AuthenticatorData authData = credential.getResponse().getAuthenticatorData();
		authData.verify(rpId, userVerification);

		publicKey.get().verifySignature(authData, clientData, credential.getResponse().getSignature());

		int signCount = authData.getSignCount();
		if (signCount > 0) {
			// TODO check signCount++
		}

	}

}
