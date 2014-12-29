package org.ironrhino.rest.client.token;

public interface TokenStore {

	public Token getToken();

	public void setToken(Token token);

}
