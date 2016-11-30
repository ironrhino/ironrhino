package org.ironrhino.rest.client.token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultTokenStore implements TokenStore {

	private Map<String, Token> map = new ConcurrentHashMap<>();

	@Override
	public Token getToken(String clientId) {
		return map.get(clientId);
	}

	@Override
	public void setToken(String clientId, Token token) {
		if (token == null)
			map.remove(clientId);
		else
			map.put(clientId, token);
	}

}
