package org.ironrhino.rest.client.token;

public class DefaultTokenStore implements TokenStore {

	private Token token;

	@Override
	public Token getToken() {
		return token;
	}

	@Override
	public void setToken(Token token) {
		this.token = token;
	}

}
