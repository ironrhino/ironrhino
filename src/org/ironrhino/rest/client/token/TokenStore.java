package org.ironrhino.rest.client.token;

public interface TokenStore {

	public Token getToken(String key);

	public void setToken(String key, Token token);

}
