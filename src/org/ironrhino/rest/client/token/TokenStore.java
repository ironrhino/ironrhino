package org.ironrhino.rest.client.token;

public interface TokenStore {

	public Token getToken(String clientId);

	public void setToken(String clientId, Token token);

}
