package org.ironrhino.rest.client.token;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;

public class CacheBasedTokenStore implements TokenStore {

	private CacheManager cacheManager;

	private String cacheKey = getClass().getSimpleName();

	private String cacheNamespace;

	private int timeToIdle = 3600;

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public String getCacheKey() {
		return cacheKey;
	}

	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public String getCacheNamespace() {
		return cacheNamespace;
	}

	public void setCacheNamespace(String cacheNamespace) {
		this.cacheNamespace = cacheNamespace;
	}

	public int getTimeToIdle() {
		return timeToIdle;
	}

	public void setTimeToIdle(int timeToIdle) {
		this.timeToIdle = timeToIdle;
	}

	@Override
	public Token getToken() {
		return (Token) cacheManager.get(cacheKey, cacheNamespace, timeToIdle, TimeUnit.SECONDS);
	}

	@Override
	public void setToken(Token token) {
		if (token == null)
			cacheManager.delete(cacheKey, cacheNamespace);
		cacheManager.put(cacheKey, token, timeToIdle, -1, TimeUnit.SECONDS, cacheNamespace);
	}

}
