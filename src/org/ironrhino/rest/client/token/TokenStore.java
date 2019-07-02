package org.ironrhino.rest.client.token;

public interface TokenStore {

	Token getToken(String key);

	void setToken(String key, Token token);

}
