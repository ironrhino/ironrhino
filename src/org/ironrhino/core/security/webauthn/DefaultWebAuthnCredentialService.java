package org.ironrhino.core.security.webauthn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.security.webauthn.domain.AttestedCredentialData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultWebAuthnCredentialService implements WebAuthnCredentialService {

	private static final String CACHE_NAMESPACE = "wa_cha"; // WebAuthn_Challenge

	@Autowired
	private CacheManager cacheManager;

	@Override
	public void putChallenge(String username, String challenge, int timeToLive) {
		cacheManager.put(username, challenge, timeToLive, TimeUnit.MILLISECONDS, CACHE_NAMESPACE);
	}

	@Override
	public String getChallenge(String username) {
		String challenge = (String) cacheManager.get(username, CACHE_NAMESPACE);
		cacheManager.delete(username, CACHE_NAMESPACE);
		return challenge;
	}

	@Override
	public void addCredentials(String username, AttestedCredentialData credential) {
		List<AttestedCredentialData> list = credentials.computeIfAbsent(username, k -> new ArrayList<>());
		list.add(credential);
	}

	@Override
	public List<AttestedCredentialData> getCredentials(String username) {
		return credentials.getOrDefault(username, Collections.emptyList());
	}

	private Map<String, List<AttestedCredentialData>> credentials = new ConcurrentHashMap<>(); // TODO

}
