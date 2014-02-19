package org.ironrhino.security.oauth.server.service;

import java.util.List;

import org.ironrhino.security.model.User;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;

public interface OAuthManager {

	long DEFAULT_EXPIRE_TIME = 14 * 24 * 3600;

	public Authorization generate(Client client, String redirectUri,
			String scope, String responseType) throws Exception;

	public Authorization reuse(Authorization authorization);

	public Authorization grant(String authorizationId, User grantor)
			throws Exception;

	public void deny(String authorizationId);

	public Authorization authenticate(String code, Client client)
			throws Exception;

	public Authorization retrieve(String accessToken);

	public Authorization refresh(String refreshToken);

	public void revoke(String accessToken);

	public void create(Authorization authorization);

	public List<Authorization> findAuthorizationsByGrantor(User grantor);

	public long getExpireTime();

	public void removeExpired();

	public void saveClient(Client client);

	public void deleteClient(Client client);

	public Client findClientById(String clientId);

	public List<Client> findClientByOwner(User user);

}
