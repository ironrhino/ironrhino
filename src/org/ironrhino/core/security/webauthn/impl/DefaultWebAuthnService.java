package org.ironrhino.core.security.webauthn.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.security.webauthn.AssertionFailedException;
import org.ironrhino.core.security.webauthn.AttestationFailedException;
import org.ironrhino.core.security.webauthn.CredentialExpiredException;
import org.ironrhino.core.security.webauthn.StoredCredentialService;
import org.ironrhino.core.security.webauthn.WebAuthnEnabled;
import org.ironrhino.core.security.webauthn.WebAuthnService;
import org.ironrhino.core.security.webauthn.domain.Attestation;
import org.ironrhino.core.security.webauthn.domain.AttestationConveyancePreference;
import org.ironrhino.core.security.webauthn.domain.AttestationStatement;
import org.ironrhino.core.security.webauthn.domain.AttestationType;
import org.ironrhino.core.security.webauthn.domain.AttestedCredential;
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
import org.ironrhino.core.security.webauthn.domain.StoredCredential;
import org.ironrhino.core.security.webauthn.domain.UserVerificationRequirement;
import org.ironrhino.core.security.webauthn.domain.cose.Algorithm;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.servlet.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@WebAuthnEnabled
@Component
public class DefaultWebAuthnService implements WebAuthnService {

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
	private StoredCredentialService credentialService;

	@Autowired
	private CacheManager cacheManager;

	private static final String CACHE_NAMESPACE = "wa_cha"; // WebAuthn_Challenge

	public void putChallenge(String username, String challenge, int timeToLive) {
		cacheManager.put(username, challenge, timeToLive, TimeUnit.MILLISECONDS, CACHE_NAMESPACE);
	}

	public String getChallenge(String username) {
		String challenge = (String) cacheManager.get(username, CACHE_NAMESPACE);
		cacheManager.delete(username, CACHE_NAMESPACE);
		return challenge;
	}

	@Override
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
		putChallenge(username, challenge, (int) timeout);
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

	@Override
	public void verifyAttestation(PublicKeyCredential<AuthenticatorAttestationResponse> credential, String username) {
		// https://www.w3.org/TR/webauthn/#registering-a-new-credential
		try {
			ClientData clientData = credential.getResponse().getClientData();
			String challenge = getChallenge(username);
			clientData.verify(PublicKeyCredentialOperationType.CREATE, rpId, challenge);

			Attestation attestation = credential.getResponse().getAttestationObject();

			AuthenticatorData authData = attestation.getAuthData();
			authData.verify(rpId, userVerification);

			AttestationStatement attStmt = attestation.getAttStmt();
			AttestationType attestationType = attestation.getFmt().verify(attStmt, authData, clientData);
			assessTrustworthiness(attestation, attestationType);

			if (credentialService.getCredentialById(credential.getId()).isPresent())
				throw new IllegalStateException("Credential already registered");

			AttestedCredential ac = authData.getAttestedCredential();
			credentialService.addCredential(new StoredCredential(ac.getCredentialId(), ac.getAaguid(),
					ac.getCredentialPublicKey(), username, authData.getSignCount()));
		} catch (Exception e) {
			throw new AttestationFailedException(e.getMessage(), e);
		}

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
			throw new IllegalArgumentException("Untrusted AttestationType: " + attestationType);
		}
	}

	@Override
	public PublicKeyCredentialRequestOptions buildRequestOptions(String username) {
		PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions();
		String challenge = Utils.generateChallenge(challengeLength);
		putChallenge(username, challenge, (int) timeout);
		options.setChallenge(challenge);
		options.setTimeout(timeout);
		options.setRpId(rpId);
		options.setUserVerification(userVerification);
		options.setAllowCredentials(credentialService.getCredentials(username).stream()
				.map(cridential -> new PublicKeyCredentialDescriptor(cridential.getCredentialId()))
				.collect(Collectors.toList()));
		return options;
	}

	@Override
	public void verifyAssertion(PublicKeyCredential<AuthenticatorAssertionResponse> credential, String username) {
		// https://www.w3.org/TR/webauthn/#verifying-assertion
		try {
			ClientData clientData = credential.getResponse().getClientData();
			String challenge = getChallenge(username);
			clientData.verify(PublicKeyCredentialOperationType.GET, rpId, challenge);

			AuthenticatorData authData = credential.getResponse().getAuthenticatorData();
			authData.verify(rpId, userVerification);

			byte[] credentialId = credential.getId();

			StoredCredential storedCredential = credentialService.getCredentialById(credentialId)
					.orElseThrow(() -> new AssertionFailedException("Unregistered credential"));
			if (!storedCredential.getUsername().equals(username))
				throw new IllegalArgumentException("username not matched");

			storedCredential.verifySignature(authData, clientData, credential.getResponse().getSignature());

			if (authData.getSignCount() > 0)
				credentialService.updateSignCount(credentialId, authData.getSignCount());
		} catch (CredentialExpiredException e) {
			throw e;
		} catch (Exception e) {
			throw new AssertionFailedException(e.getMessage(), e);
		}
	}

}
