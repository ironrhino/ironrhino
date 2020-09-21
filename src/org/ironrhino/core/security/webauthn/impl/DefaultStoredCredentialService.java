package org.ironrhino.core.security.webauthn.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.security.webauthn.CredentialExpiredException;
import org.ironrhino.core.security.webauthn.StoredCredentialService;
import org.ironrhino.core.security.webauthn.WebAuthnEnabled;
import org.ironrhino.core.security.webauthn.domain.StoredCredential;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.security.webauthn.model.WebAuthnCredential;
import org.ironrhino.core.service.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@WebAuthnEnabled
@Component
public class DefaultStoredCredentialService implements StoredCredentialService {

	@Autowired
	private EntityManager<WebAuthnCredential> entityManager;

	@Override
	public void addCredential(StoredCredential credential) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = new WebAuthnCredential(credential);
		entityManager.save(wac);
	}

	@Override
	public void updateExpiryTime(byte[] credentialId, LocalDateTime expiryTime) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = entityManager.get(Utils.encodeBase64url(credentialId));
		wac.setExpiryTime(expiryTime);
		entityManager.update(wac);
	}

	@Override
	public void updateSignCount(byte[] credentialId, int signCount) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = entityManager.get(Utils.encodeBase64url(credentialId));
		wac.setSignCount(signCount);
		entityManager.update(wac);
	}

	@Override
	public Optional<StoredCredential> getCredentialById(byte[] credentialId) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = entityManager.get(Utils.encodeBase64url(credentialId));
		if (wac != null && !wac.isNotExpired())
			throw new CredentialExpiredException("Credential expired");
		return Optional.ofNullable(wac != null ? wac.toStoredCredential() : null);
	}

	@Override
	public boolean hasCredentials(String username) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("username", username));
		return entityManager.countByCriteria(dc) > 0;
	}

	@Override
	public List<StoredCredential> getCredentials(String username) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("username", username));
		return entityManager.findListByCriteria(dc).stream()
				// .filter(WebAuthnCredential::isNotExpired)
				.map(WebAuthnCredential::toStoredCredential).collect(Collectors.toList());
	}

}
