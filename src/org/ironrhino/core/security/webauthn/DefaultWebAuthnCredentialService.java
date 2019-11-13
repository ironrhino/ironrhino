package org.ironrhino.core.security.webauthn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ironrhino.core.security.webauthn.domain.AttestedCredential;
import org.springframework.stereotype.Component;

@Component
public class DefaultWebAuthnCredentialService implements WebAuthnCredentialService {

	@Override
	public void addCredentials(String username, AttestedCredential credential) {
		List<AttestedCredential> list = credentials.computeIfAbsent(username, k -> new ArrayList<>());
		list.add(credential);
	}

	@Override
	public List<AttestedCredential> getCredentials(String username) {
		return credentials.getOrDefault(username, Collections.emptyList());
	}

	private Map<String, List<AttestedCredential>> credentials = new ConcurrentHashMap<>(); // TODO

}
