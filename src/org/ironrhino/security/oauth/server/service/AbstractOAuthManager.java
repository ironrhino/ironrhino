package org.ironrhino.security.oauth.server.service;

import org.ironrhino.security.oauth.server.enums.GrantType;
import org.ironrhino.security.oauth.server.model.Authorization;
import org.ironrhino.security.oauth.server.model.Client;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractOAuthManager implements OAuthManager {

	@Value("${oauth.authorization.lifetime:" + DEFAULT_LIFE_TIME + "}")
	protected int authorizationLifetime;

	@Value("${oauth.authorization.expireTime:" + DEFAULT_EXPIRE_TIME + "}")
	protected long expireTime;

	@Value("${oauth.authorization.exclusive:false}")
	protected boolean exclusive;

	@Value("${oauth.authorization.maximumDevices:0}")
	protected int maximumDevices;

	@Override
	public Authorization grant(Client client, String grantor, String deviceId, String deviceName) {
		if (exclusive)
			deleteAuthorizationsByGrantor(grantor, client.getId(), GrantType.password);
		return doGrant(client, grantor, deviceId, deviceName);
	}

	protected abstract Authorization doGrant(Client client, String grantor, String deviceId, String deviceName);

}
