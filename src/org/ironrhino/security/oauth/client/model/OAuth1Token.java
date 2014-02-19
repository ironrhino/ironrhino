package org.ironrhino.security.oauth.client.model;

import org.apache.commons.lang3.StringUtils;

public class OAuth1Token extends OAuthToken {

	private static final long serialVersionUID = 1320442804849307877L;

	private String token;

	private String secret;

	public OAuth1Token() {
		super();
	}

	public OAuth1Token(String source) {
		super(source);
	}

	@Override
	public void setSource(String source) {
		super.setSource(source);
		if (StringUtils.isBlank(source))
			return;
		String[] arr1 = source.split("&");
		for (String s : arr1) {
			String[] arr2 = s.split("=", 2);
			if (arr2.length > 1 && arr2[0].equals("oauth_token"))
				token = arr2[1];
			else if (arr2.length > 1 && arr2[0].equals("oauth_token_secret"))
				secret = arr2[1];
		}
		if (token == null || secret == null)
			throw new IllegalArgumentException("token or secret is null");
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

}
