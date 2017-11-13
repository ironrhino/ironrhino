package org.ironrhino.security.oauth.server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.server.domain.OAuthError;
import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;

public class RedisOAuthManager extends AbstractOAuthManager {

	@Autowired
	private Logger logger;

	@Autowired
	private RedisTemplate<String, Client> clientRedisTemplate;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

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
	public Authorization grant(Client client, String deviceId, String deviceName) {
		Client orig = findClientById(client.getClientId());
		if (orig == null)
			throw new OAuthError(OAuthError.UNAUTHORIZED_CLIENT, "client_id_not_exists");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new OAuthError(OAuthError.UNAUTHORIZED_CLIENT, "client_secret_mismatch");
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client.getId());
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setResponseType(ResponseType.token);
		auth.setGrantType(GrantType.client_credentials);
		if (StringUtils.isNotBlank(deviceId)) {
			auth.setDeviceId(deviceId);
			auth.setDeviceName(deviceName);
		}
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
	protected Authorization doGrant(Client client, String grantor, String deviceId, String deviceName) {
		if (StringUtils.isNotBlank(deviceId)) {
			List<Authorization> auths = findAuthorizationsByGrantor(grantor).stream()
					.filter(a -> a.getClient().equals(client.getClientId()) && a.getDeviceId() != null)
					.collect(Collectors.toList());
			Optional<Authorization> auth = auths.stream().filter(a -> deviceId.equals(a.getDeviceId())).findAny();
			if (auth.isPresent()) {
				return refresh(client, auth.get().getRefreshToken());
			} else if (maximumDevices > 0 && auths.size() >= maximumDevices) {
				throw new IllegalArgumentException("maximum_devices_reached");
			}
		}
		Authorization auth = new Authorization();
		if (authorizationLifetime > 0)
			auth.setLifetime(authorizationLifetime);
		auth.setId(CodecUtils.nextId());
		auth.setClient(client.getId());
		auth.setGrantor(grantor);
		auth.setRefreshToken(CodecUtils.nextId());
		auth.setResponseType(ResponseType.token);
		auth.setGrantType(GrantType.password);
		if (StringUtils.isNotBlank(deviceId)) {
			auth.setDeviceId(deviceId);
			auth.setDeviceName(deviceName);
		}
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
			throw new OAuthError(OAuthError.INVALID_GRANT, "redirect_uri_mismatch");
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
			throw new OAuthError(OAuthError.INVALID_GRANT, "bad_auth");
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
			throw new OAuthError(OAuthError.INVALID_GRANT, "code_invalid");
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(NAMESPACE_AUTHORIZATION + id),
					Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth == null)
			throw new OAuthError(OAuthError.INVALID_GRANT, "code_invalid");
		if (auth.isClientSide())
			throw new OAuthError(OAuthError.INVALID_GRANT, "not_server_side");
		if (auth.getGrantor() == null)
			throw new OAuthError(OAuthError.INVALID_GRANT, "user_not_granted");
		Client orig = findClientById(auth.getClient());
		if (!orig.getId().equals(client.getId()))
			throw new OAuthError(OAuthError.UNAUTHORIZED_CLIENT, "client_id_mismatch");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new OAuthError(OAuthError.UNAUTHORIZED_CLIENT, "client_secret_mismatch");
		if (!orig.supportsRedirectUri(client.getRedirectUri()))
			throw new OAuthError(OAuthError.INVALID_GRANT, "redirect_uri_mismatch");
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
			throw new OAuthError(OAuthError.UNAUTHORIZED_CLIENT, "client_id_not_exists");
		if (!orig.getSecret().equals(client.getSecret()))
			throw new OAuthError(OAuthError.UNAUTHORIZED_CLIENT, "client_secret_mismatch");
		String keyRefreshToken = NAMESPACE_AUTHORIZATION + refreshToken;
		String id = stringRedisTemplate.opsForValue().get(keyRefreshToken);
		if (id == null)
			throw new OAuthError(OAuthError.INVALID_GRANT);
		Authorization auth = null;
		try {
			auth = JsonUtils.fromJson(stringRedisTemplate.opsForValue().get(NAMESPACE_AUTHORIZATION + id),
					Authorization.class);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		if (auth == null)
			throw new OAuthError(OAuthError.INVALID_GRANT);
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

	public void saveClient(Client client) {
		if (client.isNew())
			client.setId(CodecUtils.nextId());
		clientRedisTemplate.opsForValue().set(NAMESPACE_CLIENT + client.getId(), client);
		if (client.getOwner() != null)
			stringRedisTemplate.opsForSet().add(NAMESPACE_CLIENT_OWNER + client.getOwner().getUsername(),
					client.getId());
	}

	public void deleteClient(Client client) {
		if (client.isNew())
			return;
		clientRedisTemplate.delete(NAMESPACE_CLIENT + client.getId());
		if (client.getOwner() != null)
			stringRedisTemplate.opsForSet().remove(NAMESPACE_CLIENT_OWNER + client.getOwner().getUsername(),
					client.getId());
	}

	@Override
	public Client findClientById(String clientId) {
		if (StringUtils.isBlank(clientId))
			return null;
		Client c = clientRedisTemplate.opsForValue().get(NAMESPACE_CLIENT + clientId);
		return c != null && c.isEnabled() ? c : null;
	}

	@Override
	public List<Client> findClientByOwner(UserDetails owner) {
		Set<String> ids = stringRedisTemplate.opsForSet().members(NAMESPACE_CLIENT_OWNER + owner.getUsername());
		if (ids == null || ids.isEmpty())
			return Collections.emptyList();
		List<String> keys = new ArrayList<>(ids.size());
		for (String id : ids)
			keys.add(NAMESPACE_CLIENT + id);
		List<Client> list = clientRedisTemplate.opsForValue().multiGet(keys);
		list.sort(Comparator.comparing(Client::getCreateDate));
		return list;
	}

}
