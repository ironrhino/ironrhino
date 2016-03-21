package org.ironrhino.security.oauth.server.service;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("oauthManager")
@ServiceImplementationConditional(profiles = CLUSTER)
public class HybirdOAuthManager extends AbstractOAuthManager {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ClientManager clientManager;

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	private static final String NAMESPACE_AUTHORIZATION = "oauth:authorization:";
	private static final String NAMESPACE_AUTHORIZATION_GRANTOR = "oauth:authorization:grantor:";

	// oauth:authorization:{id} -> authorization
	// oauth:authorization:{code} -> id
	// oauth:authorization:{accessToken} -> id
	// oauth:authorization:{refreshToken} -> id
	// oauth:authorization:grantor:{username} -> [id]

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public Authorization grant(Client client) {
		Client orig = findClientById(client.getClientId());
		if (orig == null)
			throw new IllegalArgumentException("client_id_not_exists");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new IllegalArgumentException("client_secret_mismatch");
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client.getId());
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setResponseType(ResponseType.token);
		auth.setGrantType(GrantType.client_credential);
		try {
			auth.setAddress(RequestContext.getRequest().getRemoteAddr());
		} catch (NullPointerException npe) {
		}
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getId(), JsonUtils.toJson(auth),
				expireTime, TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization grant(Client client, String grantor) {
		if (exclusive)
			deleteAuthorizationsByGrantor(grantor, client.getId(), GrantType.password);
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client.getId());
		auth.setGrantor(grantor);
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setResponseType(ResponseType.token);
		auth.setGrantType(GrantType.password);
		try {
			auth.setAddress(RequestContext.getRequest().getRemoteAddr());
		} catch (NullPointerException npe) {
		}
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getId(), JsonUtils.toJson(auth),
				expireTime, TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForList().leftPush(NAMESPACE_AUTHORIZATION_GRANTOR + auth.getGrantor(), auth.getId());
		return auth;
	}

	@Override
	public Authorization generate(Client client, String redirectUri, String scope, ResponseType responseType) {
		if (!client.supportsRedirectUri(redirectUri))
			throw new IllegalArgumentException("redirect_uri_mismatch");
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client.getId());
		if (StringUtils.isNotBlank(scope))
			auth.setScope(scope);
		if (responseType != null)
			auth.setResponseType(responseType);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getId(), JsonUtils.toJson(auth),
				expireTime, TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization reuse(Authorization auth) {
		auth.setCode(CodecUtils.nextId());
		auth.setModifyDate(new Date());
		auth.setLifetime(Authorization.DEFAULT_LIFETIME);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getId(), JsonUtils.toJson(auth),
				expireTime, TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getCode(), auth.getId(), expireTime,
				TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization grant(String authorizationId, String grantor) {
		String key = NAMESPACE_AUTHORIZATION + authorizationId;
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(key), Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth == null)
			throw new IllegalArgumentException("bad_auth");
		auth.setGrantor(grantor);
		try {
			auth.setAddress(RequestContext.getRequest().getRemoteAddr());
		} catch (NullPointerException npe) {
		}
		auth.setModifyDate(new Date());
		if (auth.isClientSide()) {
			stringRedisTemplate.delete(key);
			stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
					auth.getExpiresIn(), TimeUnit.SECONDS);
			stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
					auth.getExpiresIn(), TimeUnit.SECONDS);
		} else {
			auth.setCode(CodecUtils.nextId());
			stringRedisTemplate.delete(key);
			stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getCode(), auth.getId(), expireTime,
					TimeUnit.SECONDS);
		}
		stringRedisTemplate.opsForList().leftPush(NAMESPACE_AUTHORIZATION_GRANTOR + auth.getGrantor(), auth.getId());
		return auth;
	}

	@Override
	public void deny(String authorizationId) {
		stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + authorizationId);
	}

	@Override
	public Authorization authenticate(String code, Client client) {
		String key = NAMESPACE_AUTHORIZATION + code;
		String id = stringRedisTemplate.opsForValue().get(key);
		if (id == null)
			throw new IllegalArgumentException("code_invalid");
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(NAMESPACE_AUTHORIZATION + id),
					Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth == null)
			throw new IllegalArgumentException("code_invalid");
		if (auth.isClientSide())
			throw new IllegalArgumentException("not_server_side");
		if (auth.getGrantor() == null)
			throw new IllegalArgumentException("user_not_granted");
		Client orig = findClientById(auth.getClient());
		if (!orig.getId().equals(client.getId()))
			throw new IllegalArgumentException("client_id_mismatch");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new IllegalArgumentException("client_secret_mismatch");
		if (!orig.supportsRedirectUri(client.getRedirectUri()))
			throw new IllegalArgumentException("redirect_uri_mismatch");
		if (exclusive)
			deleteAuthorizationsByGrantor(auth.getGrantor(), client.getId(), GrantType.authorization_code);
		auth.setCode(null);
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setGrantType(GrantType.authorization_code);
		auth.setModifyDate(new Date());
		stringRedisTemplate.delete(key);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getId(), JsonUtils.toJson(auth),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getRefreshToken(),
				NAMESPACE_AUTHORIZATION + auth.getId(), auth.getExpiresIn(), TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public Authorization retrieve(String accessToken) {
		String key = NAMESPACE_AUTHORIZATION + accessToken;
		String id = stringRedisTemplate.opsForValue().get(key);
		if (id == null)
			return null;
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(NAMESPACE_AUTHORIZATION + id),
					Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth != null && auth.getExpiresIn() < 0)
			return null;
		return auth;
	}

	@Override
	public Authorization refresh(Client client, String refreshToken) {
		Client orig = findClientById(client.getClientId());
		if (orig == null)
			throw new IllegalArgumentException("client_id_not_exists");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new IllegalArgumentException("client_secret_mismatch");
		String keyRefreshToken = NAMESPACE_AUTHORIZATION + refreshToken;
		String id = stringRedisTemplate.opsForValue().get(keyRefreshToken);
		if (id == null)
			throw new IllegalArgumentException("invalid_token");
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(NAMESPACE_AUTHORIZATION + id),
					Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth == null)
			throw new IllegalArgumentException("invalid_token");
		stringRedisTemplate.delete(keyRefreshToken);
		stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + auth.getAccessToken());
		auth.setAccessToken(CodecUtils.nextId());
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setModifyDate(new Date());
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		return auth;
	}

	@Override
	public boolean revoke(String accessToken) {
		String key = NAMESPACE_AUTHORIZATION + accessToken;
		String id = stringRedisTemplate.opsForValue().get(key);
		if (id == null)
			return false;
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(NAMESPACE_AUTHORIZATION + id),
					Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth != null) {
			stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + auth.getId());
			stringRedisTemplate.delete(key);
			stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + auth.getRefreshToken());
			stringRedisTemplate.opsForList().remove(NAMESPACE_AUTHORIZATION_GRANTOR + auth.getGrantor(), 0,
					auth.getId());
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void create(Authorization auth) {
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getAccessToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForValue().set(NAMESPACE_AUTHORIZATION + auth.getRefreshToken(), auth.getId(),
				auth.getExpiresIn(), TimeUnit.SECONDS);
		stringRedisTemplate.opsForList().leftPush(NAMESPACE_AUTHORIZATION_GRANTOR + auth.getGrantor(), auth.getId());
	}

	@Override
	public List<Authorization> findAuthorizationsByGrantor(String grantor) {
		String keyForList = NAMESPACE_AUTHORIZATION_GRANTOR + grantor;
		List<String> tokens = stringRedisTemplate.opsForList().range(keyForList, 0, -1);
		if (tokens == null || tokens.isEmpty())
			return Collections.emptyList();
		List<String> keys = new ArrayList<>(tokens.size());
		for (String token : tokens)
			keys.add(NAMESPACE_AUTHORIZATION + token);
		List<String> list = stringRedisTemplate.opsForValue().multiGet(keys);
		List<Authorization> result = new ArrayList<>(list.size());
		for (String json : list) {
			try {
				result.add(JsonUtils.fromJson(json, Authorization.class));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return result;
	}

	@Override
	public void deleteAuthorizationsByGrantor(String grantor, String client, GrantType grantType) {
		List<Authorization> list = findAuthorizationsByGrantor(grantor);
		for (Authorization authorization : list)
			if ((client == null || client.equals(authorization.getClient()))
					&& (grantType == null || grantType == authorization.getGrantType())) {
				stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + authorization.getId());
				stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + authorization.getAccessToken());
				stringRedisTemplate.delete(NAMESPACE_AUTHORIZATION + authorization.getRefreshToken());
				stringRedisTemplate.opsForList().remove(NAMESPACE_AUTHORIZATION_GRANTOR + grantor, 0,
						authorization.getId());
			}
	}

	@Override
	public Client findClientById(String clientId) {
		if (StringUtils.isBlank(clientId))
			return null;
		Client c = clientManager.get(clientId);
		return c != null && c.isEnabled() ? c : null;
	}

	@Override
	public List<Client> findClientByOwner(UserDetails owner) {
		DetachedCriteria dc = clientManager.detachedCriteria();
		dc.add(Restrictions.eq("owner", owner));
		dc.addOrder(Order.asc("createDate"));
		return clientManager.findListByCriteria(dc);
	}

}
