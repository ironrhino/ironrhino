package org.ironrhino.rest.client.token;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;

import lombok.Getter;
import lombok.Setter;

public class CacheBasedTokenStore implements TokenStore {

	@Getter
	@Setter
	private CacheManager cacheManager;

	@Getter
	@Setter
	private String cacheNamespace = "Token";

	@Override
	public Token getToken(String key) {
		return (Token) cacheManager.get(key, cacheNamespace);
	}

	@Override
	public void setToken(String key, Token token) {
		if (token == null) {
			cacheManager.delete(key, cacheNamespace);
		} else {
			int timeToLive = token.getExpiresIn();
			if (token.getRefreshToken() != null)
				timeToLive += 36000;
			cacheManager.put(key, token, timeToLive, TimeUnit.SECONDS, cacheNamespace);
		}
	}

}
