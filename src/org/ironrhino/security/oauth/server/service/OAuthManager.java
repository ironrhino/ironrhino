package org.ironrhino.security.oauth.server.service;

import java.util.List;

import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.ironrhino.security.oauth.server.model.GrantType;
import org.ironrhino.security.oauth.server.model.ResponseType;
import org.springframework.security.core.userdetails.UserDetails;

public interface OAuthManager {

	long DEFAULT_EXPIRE_TIME = 14 * 24 * 3600;

	public Authorization grant(Client client);

	public Authorization grant(Client client, UserDetails grantor);

	public Authorization generate(Client client, String redirectUri, String scope, ResponseType responseType)
			throws Exception;

	public Authorization reuse(Authorization authorization);

	public Authorization grant(String authorizationId, UserDetails grantor) throws Exception;

	public void deny(String authorizationId);

	public Authorization authenticate(String code, Client client) throws Exception;

	public Authorization retrieve(String accessToken);

	public Authorization refresh(Client client, String refreshToken);

	public void revoke(String accessToken);

	public void create(Authorization authorization);

	public List<Authorization> findAuthorizationsByGrantor(UserDetails grantor);

	public void deleteAuthorizationsByGrantor(UserDetails grantor, GrantType grantType);

	public long getExpireTime();

	public Client findClientById(String clientId);

	public List<Client> findClientByOwner(UserDetails user);

}
