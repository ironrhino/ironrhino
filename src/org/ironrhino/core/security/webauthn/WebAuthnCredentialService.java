package org.ironrhino.core.security.webauthn;

import java.util.List;

import org.ironrhino.core.security.webauthn.domain.AttestedCredentialData;

public interface WebAuthnCredentialService {

	public void putChallenge(String username, String challenge, int timeToLive);

	public String getChallenge(String username);

	public void addCredentials(String username, AttestedCredentialData credential);

	public List<AttestedCredentialData> getCredentials(String username);

}
