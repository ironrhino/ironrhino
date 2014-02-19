package org.ironrhino.security.oauth.server.service;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DEFAULT;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.metadata.Trigger;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component("oauthManager")
@Profile({ DEFAULT, DUAL, CLOUD })
@SuppressWarnings({ "unchecked", "rawtypes" })
public class OAuthManagerImpl implements OAuthManager {

	@Resource
	private EntityManager entityManager;

	private long expireTime = DEFAULT_EXPIRE_TIME;

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public long getExpireTime() {
		return expireTime;
	}

	@Override
	public Authorization generate(Client client, String redirectUri,
			String scope, String responseType) {
		if (!client.supportsRedirectUri(redirectUri))
			throw new IllegalArgumentException("REDIRECT_URI_MISMATCH");
		Authorization auth = new Authorization();
		auth.setClient(client);
		if (StringUtils.isNotBlank(scope))
			auth.setScope(scope);
		if (StringUtils.isNotBlank(responseType))
			auth.setResponseType(responseType);
		entityManager.save(auth);
		return auth;
	}

	@Override
	public Authorization reuse(Authorization auth) {
		auth.setCode(CodecUtils.nextId());
		auth.setModifyDate(new Date());
		auth.setLifetime(Authorization.DEFAULT_LIFETIME);
		entityManager.save(auth);
		return auth;
	}

	@Override
	public Authorization grant(String authorizationId, User grantor) {
		entityManager.setEntityClass(Authorization.class);
		Authorization auth = (Authorization) entityManager.get(authorizationId);
		if (auth == null)
			throw new IllegalArgumentException("BAD_AUTH");
		auth.setGrantor(grantor);
		auth.setModifyDate(new Date());
		if (!auth.isClientSide())
			auth.setCode(CodecUtils.nextId());
		entityManager.save(auth);
		return auth;
	}

	@Override
	public void deny(String authorizationId) {
		entityManager.setEntityClass(Authorization.class);
		Authorization auth = (Authorization) entityManager.get(authorizationId);
		if (auth != null)
			entityManager.delete(auth);
	}

	@Override
	public Authorization authenticate(String code, Client client) {
		entityManager.setEntityClass(Authorization.class);
		Authorization auth = (Authorization) entityManager
				.findOne("code", code);
		if (auth == null)
			throw new IllegalArgumentException("CODE_INVALID");
		if (auth.isClientSide())
			throw new IllegalArgumentException("NOT_SERVER_SIDE");
		if (auth.getGrantor() == null)
			throw new IllegalArgumentException("USER_NOT_GRANTED");
		Client orig = auth.getClient();
		if (!orig.getId().equals(client.getId()))
			throw new IllegalArgumentException("CLIENT_ID_MISMATCH");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new IllegalArgumentException("CLIENT_SECRET_MISMATCH");
		if (!orig.supportsRedirectUri(client.getRedirectUri()))
			throw new IllegalArgumentException("REDIRECT_URI_MISMATCH");
		auth.setCode(null);
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setModifyDate(new Date());
		entityManager.save(auth);
		return auth;
	}

	@Override
	public Authorization retrieve(String accessToken) {
		entityManager.setEntityClass(Authorization.class);
		Authorization auth = (Authorization) entityManager
				.findByNaturalId(accessToken);
		if (auth != null) {
			if (auth.getClient() != null && !auth.getClient().isEnabled()) {
				entityManager.delete(auth);
				return null;
			}
			if (auth.getExpiresIn() < 0)
				return null;
		}
		return auth;
	}

	@Override
	public Authorization refresh(String refreshToken) {
		entityManager.setEntityClass(Authorization.class);
		Authorization auth = (Authorization) entityManager.findOne(
				"refreshToken", refreshToken);
		if (auth != null) {
			if (auth.getClient() != null && !auth.getClient().isEnabled()) {
				entityManager.delete(auth);
				return null;
			}
			auth.setAccessToken(CodecUtils.nextId());
			auth.setModifyDate(new Date());
			entityManager.save(auth);
		}
		return auth;
	}

	@Override
	public void revoke(String accessToken) {
		entityManager.setEntityClass(Authorization.class);
		Authorization auth = (Authorization) entityManager
				.findByNaturalId(accessToken);
		if (auth != null)
			entityManager.delete(auth);
	}

	@Override
	public void create(Authorization authorization) {
		entityManager.save(authorization);
	}

	@Override
	public List<Authorization> findAuthorizationsByGrantor(User grantor) {
		entityManager.setEntityClass(Authorization.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("grantor", grantor));
		dc.addOrder(Order.desc("modifyDate"));
		return entityManager.findListByCriteria(dc);
	}

	@Override
	@Trigger
	@Scheduled(cron = "0 30 23 * * ?")
	public void removeExpired() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, (int) (-expireTime));
		entityManager
				.executeUpdate(
						"delete from Authorization a where lifetime >0 and a.modifyDate < ?1",
						cal.getTime());
	}

	@Override
	public void saveClient(Client client) {
		entityManager.save(client);
	}

	@Override
	public void deleteClient(Client client) {
		entityManager.delete(client);
	}

	@Override
	public Client findClientById(String clientId) {
		entityManager.setEntityClass(Client.class);
		Client c = (Client) entityManager.get(clientId);
		return c != null && c.isEnabled() ? c : null;
	}

	@Override
	public List<Client> findClientByOwner(User owner) {
		entityManager.setEntityClass(Client.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("owner", owner));
		dc.addOrder(Order.asc("createDate"));
		return entityManager.findListByCriteria(dc);
	}
}
