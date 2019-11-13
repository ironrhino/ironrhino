package org.ironrhino.core.security.webauthn;

import java.util.List;

import org.ironrhino.core.security.webauthn.domain.AttestedCredential;

public interface WebAuthnCredentialService {

	public void addCredentials(String username, AttestedCredential credential);

	public List<AttestedCredential> getCredentials(String username);

}
