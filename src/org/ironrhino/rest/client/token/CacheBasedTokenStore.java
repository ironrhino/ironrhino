package org.ironrhino.rest.client.token;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;

public class CacheBasedTokenStore implements TokenStore {

	private CacheManager cacheManager;

	private String cacheNamespace = "Token";

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public String getCacheNamespace() {
		return cacheNamespace;
	}

	public void setCacheNamespace(String cacheNamespace) {
		this.cacheNamespace = cacheNamespace;
	}

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
