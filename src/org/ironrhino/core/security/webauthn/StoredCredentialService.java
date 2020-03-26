package org.ironrhino.core.security.webauthn;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.ironrhino.core.security.webauthn.domain.StoredCredential;

public interface StoredCredentialService {

	void addCredential(StoredCredential credential);

	default void updateExpiryTime(byte[] credentialId, LocalDateTime expiryTime) {
		// ignore;
	}

	void updateSignCount(byte[] credentialId, int signCount);

	Optional<StoredCredential> getCredentialById(byte[] credentialId);

	boolean hasCredentials(String username);

	List<StoredCredential> getCredentials(String username);

}
