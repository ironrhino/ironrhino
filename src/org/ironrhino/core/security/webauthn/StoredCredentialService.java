package org.ironrhino.core.security.webauthn;

import java.util.List;
import java.util.Optional;

import org.ironrhino.core.security.webauthn.domain.StoredCredential;

public interface StoredCredentialService {

	public void addCredential(StoredCredential credential);

	public void updateSignCount(byte[] credentialId, int signCount);

	public Optional<StoredCredential> getCredentialById(byte[] credentialId);

	public List<StoredCredential> getCredentials(String username);

}
