package org.ironrhino.security.oauth.server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisOAuthManagerImpl implements OAuthManager {

	@Value("${oauth.authorization.lifetime:0}")
	private int authorizationLifetime;

	@Value("${oauth.authorization.expireTime:" + DEFAULT_EXPIRE_TIME + "}")
	private long expireTime;

	private RedisTemplate<String, Authorization> authorizationRedisTemplate;

	private RedisTemplate<String, Client> clientRedisTemplate;

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	private static final String NAMESPACE_AUTHORIZATION = "oauth:authorization:";
	private static final String NAMESPACE_AUTHORIZATION_GRANTOR = "oauth:authorization:grantor:";

	private static final String NAMESPACE_CLIENT = "oauth:client:";
	private static final String NAMESPACE_CLIENT_OWNER = "oauth:client:owner:";

	// oauth:authorization:{id} -> authorization
	// oauth:authorization:{code} -> id
	// oauth:authorization:{accessToken} -> id
	// oauth:authorization:{refreshToken} -> id
	// oauth:authorization:grantor:{username} -> [id]

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public long getExpireTime() {
		return expireTime;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setRedisTemplate(RedisTemplate redisTemplate) {
		this.authorizationRedisTemplate = redisTemplate;
		this.clientRedisTemplate = redisTemplate;
	}

	@Override
	public Authorization grant(Client client) {
		Client orig = findClientById(client.getClientId());
		if (orig == null)
			throw new IllegalArgumentException("CLIENT_ID_NOT_EXISTS");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new IllegalArgumentException("CLIENT_SECRET_MISMATCH");
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client);
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setResponseType("token");
		authorizationRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getId(), auth, expireTime,
				TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization grant(Client client, User grantor) {
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client);
		auth.setGrantor(grantor);
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setResponseType("token");
		authorizationRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getId(), auth, expireTime,
				TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForList().leftPush(
				NAMESPACE_AUTHORIZATION_GRANTOR
						+ auth.getGrantor().getUsername(), auth.getId());
		return auth;
	}

	@Override
	public Authorization generate(Client client, String redirectUri,
			String scope, String responseType) {
		if (!client.supportsRedirectUri(redirectUri))
			throw new IllegalArgumentException("REDIRECT_URI_MISMATCH");
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client);
		if (StringUtils.isNotBlank(scope))
			auth.setScope(scope);
		if (StringUtils.isNotBlank(responseType))
			auth.setResponseType(responseType);
		authorizationRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getId(), auth, expireTime,
				TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization reuse(Authorization auth) {
		auth.setCode(CodecUtils.nextId());
		auth.setModifyDate(new Date());
		auth.setLifetime(Authorization.DEFAULT_LIFETIME);
		authorizationRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getId(), auth, expireTime,
				TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getCode(), auth.getId(),
				expireTime, TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization grant(String authorizationId, User grantor) {
		String key = NAMESPACE_AUTHORIZATION + authorizationId;
		Authorization auth = authorizationRedisTemplate.opsForValue().get(key);
		if (auth == null)
			throw new IllegalArgumentException("BAD_AUTH");
		auth.setGrantor(grantor);
		auth.setModifyDate(new Date());
		if (auth.isClientSide()) {
			authorizationRedisTemplate.delete(key);
			stringRedisTemplate.opsForValue().set(
					NAMESPACE_AUTHORIZATION + auth.getAccessToken(),
					auth.getId(), auth.getExpiresIn(), TimeUnit.SECONDS);
			stringRedisTemplate.opsForValue().set(
					NAMESPACE_AUTHORIZATION + auth.getRefreshToken(),
					auth.getId(), auth.getExpiresIn(), TimeUnit.SECONDS);
		} else {
			auth.setCode(CodecUtils.nextId());
			authorizationRedisTemplate.delete(key);
			stringRedisTemplate.opsForValue().set(
					NAMESPACE_AUTHORIZATION + auth.getCode(), auth.getId(),
					expireTime, TimeUnit.SECONDS);
		}
		stringRedisTemplate.opsForList().leftPush(
				NAMESPACE_AUTHORIZATION_GRANTOR
						+ auth.getGrantor().getUsername(), auth.getId());
		return auth;
	}

	@Override
	public void deny(String authorizationId) {
		authorizationRedisTemplate.delete(NAMESPACE_AUTHORIZATION
				+ authorizationId);
	}

	@Override
	public Authorization authenticate(String code, Client client) {
		String key = NAMESPACE_AUTHORIZATION + code;
		String id = stringRedisTemplate.opsForValue().get(key);
		if (id == null)
			throw new IllegalArgumentException("CODE_INVALID");
		Authorization auth = authorizationRedisTemplate.opsForValue().get(
				NAMESPACE_AUTHORIZATION + id);
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
		authorizationRedisTemplate.delete(key);
		authorizationRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getId(), auth,
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getRefreshToken(),
				NAMESPACE_AUTHORIZATION + auth.getId(), auth.getExpiresIn(),
				TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization retrieve(String accessToken) {
		String key = NAMESPACE_AUTHORIZATION + accessToken;
		String id = stringRedisTemplate.opsForValue().get(key);
		if (id == null)
			return null;
		Authorization auth = authorizationRedisTemplate.opsForValue().get(
				NAMESPACE_AUTHORIZATION + id);
		if (auth != null && auth.getExpiresIn() < 0)
			return null;
		return auth;
	}

	@Override
	public Authorization refresh(Client client, String refreshToken) {
		Client orig = findClientById(client.getClientId());
		if (orig == null)
			throw new IllegalArgumentException("CLIENT_ID_NOT_EXISTS");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new IllegalArgumentException("CLIENT_SECRET_MISMATCH");
		String keyRefreshToken = NAMESPACE_AUTHORIZATION + refreshToken;
		String id = stringRedisTemplate.opsForValue().get(keyRefreshToken);
		if (id == null)
			throw new IllegalArgumentException("INVALID_TOKEN");
		Authorization auth = authorizationRedisTemplate.opsForValue().get(
				NAMESPACE_AUTHORIZATION + id);
		if (auth == null)
			throw new IllegalArgumentException("INVALID_TOKEN");
		authorizationRedisTemplate.delete(keyRefreshToken);
		authorizationRedisTemplate.delete(NAMESPACE_AUTHORIZATION
				+ auth.getAccessToken());
		auth.setAccessToken(CodecUtils.nextId());
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setModifyDate(new Date());
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public void revoke(String accessToken) {
		String key = NAMESPACE_AUTHORIZATION + accessToken;
		String id = stringRedisTemplate.opsForValue().get(key);
		if (id == null)
			return;
		Authorization auth = authorizationRedisTemplate.opsForValue().get(
				NAMESPACE_AUTHORIZATION + id);
		if (auth != null) {
			authorizationRedisTemplate.delete(NAMESPACE_AUTHORIZATION
					+ auth.getId());
			authorizationRedisTemplate.delete(key);
			authorizationRedisTemplate.delete(NAMESPACE_AUTHORIZATION
					+ auth.getRefreshToken());
			stringRedisTemplate.opsForList().remove(
					NAMESPACE_AUTHORIZATION_GRANTOR
							+ auth.getGrantor().getUsername(), 0, auth.getId());
		}
	}

	@Override
	public void create(Authorization auth) {
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(
				NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForList().leftPush(
				NAMESPACE_AUTHORIZATION_GRANTOR
						+ auth.getGrantor().getUsername(), auth.getId());
	}

	@Override
	public List<Authorization> findAuthorizationsByGrantor(User grantor) {
		String keyForList = NAMESPACE_AUTHORIZATION_GRANTOR
				+ grantor.getUsername();
		List<String> tokens = stringRedisTemplate.opsForList().range(
				keyForList, 0, -1);
		if (tokens == null || tokens.isEmpty())
			return Collections.emptyList();
		List<String> keys = new ArrayList<String>(tokens.size());
		for (String token : tokens)
			keys.add(NAMESPACE_AUTHORIZATION + token);
		return authorizationRedisTemplate.opsForValue().multiGet(keys);
	}

	@Override
	public void removeExpired() {
	}

	@Override
	public void saveClient(Client client) {
		if (client.isNew())
			client.setId(CodecUtils.nextId());
		clientRedisTemplate.opsForValue().set(
				NAMESPACE_CLIENT + client.getId(), client);
		if (client.getOwner() != null)
			stringRedisTemplate.opsForSet().add(
					NAMESPACE_CLIENT_OWNER + client.getOwner().getUsername(),
					client.getId());
	}

	@Override
	public void deleteClient(Client client) {
		if (client.isNew())
			return;
		clientRedisTemplate.delete(NAMESPACE_CLIENT + client.getId());
		if (client.getOwner() != null)
			stringRedisTemplate.opsForSet().remove(
					NAMESPACE_CLIENT_OWNER + client.getOwner().getUsername(),
					client.getId());
	}

	@Override
	public Client findClientById(String clientId) {
		Client c = clientRedisTemplate.opsForValue().get(
				NAMESPACE_CLIENT + clientId);
		return c != null && c.isEnabled() ? c : null;
	}

	@Override
	public List<Client> findClientByOwner(User owner) {
		Set<String> ids = stringRedisTemplate.opsForSet().members(
				NAMESPACE_CLIENT_OWNER + owner.getUsername());
		if (ids == null || ids.isEmpty())
			return Collections.emptyList();
		List<String> keys = new ArrayList<String>(ids.size());
		for (String id : ids)
			keys.add(NAMESPACE_CLIENT + id);
		List<Client> list = clientRedisTemplate.opsForValue().multiGet(keys);
		Collections.sort(list, new Comparator<Client>() {
			@Override
			public int compare(Client o1, Client o2) {
				return o1.getCreateDate().compareTo(o2.getCreateDate());
			}
		});
		return list;
	}

}
