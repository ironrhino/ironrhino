package org.ironrhino.rest.client.token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultTokenStore implements TokenStore {

	private Map<String, Token> map = new ConcurrentHashMap<>();

	@Override
	public Token getToken(String key) {
		return map.get(key);
	}

	@Override
	public void setToken(String key, Token token) {
		if (token == null)
			map.remove(key);
		else
			map.put(key, token);
	}

}
