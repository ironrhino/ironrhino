package org.ironrhino.rest.client.token;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;

import lombok.Getter;
import lombok.Setter;

public class CacheBasedTokenStore implements TokenStore {

	@Getter
	private final CacheManager cacheManager;

	@Getter
	@Setter
	private String cacheNamespace = "token";

	public CacheBasedTokenStore(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
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
			int expiresIn = token.getExpiresIn();
			int offset = expiresIn > 3600 ? expiresIn / 20 : 300;
			expiresIn -= offset;
			if (token.getRefreshToken() != null)
				expiresIn += 36000;
			cacheManager.put(key, token, expiresIn, TimeUnit.SECONDS, cacheNamespace);
		}
	}

}
