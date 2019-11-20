package org.ironrhino.core.security.webauthn;

import org.ironrhino.core.security.webauthn.domain.AuthenticatorAssertionResponse;
import org.ironrhino.core.security.webauthn.domain.AuthenticatorAttestationResponse;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredential;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialCreationOptions;
import org.ironrhino.core.security.webauthn.domain.PublicKeyCredentialRequestOptions;

public interface WebAuthnService {

	public PublicKeyCredentialCreationOptions buildCreationOptions(String id, String username, String name);

	public void verifyAttestation(PublicKeyCredential<AuthenticatorAttestationResponse> credential, String username);

	public PublicKeyCredentialRequestOptions buildRequestOptions(String username);

	public void verifyAssertion(PublicKeyCredential<AuthenticatorAssertionResponse> credential, String username);

}
