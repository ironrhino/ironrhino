package org.ironrhino.core.security.webauthn;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.security.webauthn.domain.StoredCredential;
import org.ironrhino.core.security.webauthn.internal.Utils;
import org.ironrhino.core.security.webauthn.model.WebAuthnCredential;
import org.ironrhino.core.service.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultStoredCredentialService implements StoredCredentialService {

	@Autowired
	private EntityManager<WebAuthnCredential> entityManager;

	@Override
	public void addCredentials(StoredCredential credential) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = new WebAuthnCredential(credential);
		entityManager.save(wac);
	}

	@Override
	public void updateSignCount(byte[] credentialId, int signCount) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = entityManager.get(Utils.encodeBase64url(credentialId));
		wac.setSignCount(signCount);
		entityManager.save(wac);
	}

	@Override
	public Optional<StoredCredential> getCredentialById(byte[] credentialId) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		WebAuthnCredential wac = entityManager.get(Utils.encodeBase64url(credentialId));
		return Optional.ofNullable(wac != null ? wac.toStoredCredential() : null);
	}

	@Override
	public List<StoredCredential> getCredentials(String username) {
		entityManager.setEntityClass(WebAuthnCredential.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("username", username));
		return entityManager.findListByCriteria(dc).stream().map(WebAuthnCredential::toStoredCredential)
				.collect(Collectors.toList());
	}

}
