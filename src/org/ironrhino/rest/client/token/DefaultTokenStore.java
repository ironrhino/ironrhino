package org.ironrhino.rest.client.token;

public class DefaultTokenStore implements TokenStore {

	private Token token;

	public Token getToken() {
		return token;
	}

	public void setToken(Token token) {
		this.token = token;
	}

}
