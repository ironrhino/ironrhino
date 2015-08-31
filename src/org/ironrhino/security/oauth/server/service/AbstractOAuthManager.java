package org.ironrhino.security.oauth.server.service;

import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractOAuthManager implements OAuthManager {

	@Value("${oauth.authorization.lifetime:3600}")
	protected int authorizationLifetime;

	@Value("${oauth.authorization.expireTime:" + DEFAULT_EXPIRE_TIME + "}")
	protected long expireTime;

	@Value("${oauth.authorization.exclusive:false}")
	protected boolean exclusive;

}
