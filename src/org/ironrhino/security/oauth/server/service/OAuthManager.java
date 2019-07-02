package org.ironrhino.security.oauth.server.service;

import java.util.List;

import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.enums.ResponseType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.security.core.userdetails.UserDetails;

public interface OAuthManager {

	int DEFAULT_LIFE_TIME = 3600;

	int DEFAULT_EXPIRE_TIME = 14 * 24 * 3600;

	default Authorization grant(Client client) {
		return grant(client, null, null);
	}

	Authorization grant(Client client, String deviceId, String deviceName);

	default Authorization grant(Client client, String grantor) {
		return grant(client, grantor, null, null);
	}

	Authorization grant(Client client, String grantor, String deviceId, String deviceName);

	Authorization generate(Client client, String redirectUri, String scope, ResponseType responseType);

	Authorization reuse(Authorization authorization);

	Authorization grant(String authorizationId, String grantor);

	void deny(String authorizationId);

	Authorization authenticate(String code, Client client);

	Authorization retrieve(String accessToken);

	Authorization refresh(Client client, String refreshToken);

	boolean revoke(String accessToken);

	void create(Authorization authorization);

	List<Authorization> findAuthorizationsByGrantor(String grantor);

	void deleteAuthorizationsByGrantor(String grantor, String client, GrantType grantType);

	Client findClientById(String clientId);

	List<Client> findClientByOwner(UserDetails user);

}
