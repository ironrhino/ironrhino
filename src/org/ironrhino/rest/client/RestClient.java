package org.ironrhino.rest.client;

import java.util.HashMap;
import java.util.Map;

import org.ironrhino.rest.client.token.DefaultTokenStore;
import org.ironrhino.rest.client.token.Token;
import org.ironrhino.rest.client.token.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class RestClient {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected String accessTokenEndpoint;

	protected String clientId;

	protected String clientSecret;

	protected RestTemplate restTemplate = new org.ironrhino.rest.client.RestTemplate(this);

	protected RestTemplate internalRestTemplate = new RestTemplate();

	protected TokenStore tokenStore = new DefaultTokenStore();

	public RestClient() {

	}

	public RestClient(String accessTokenEndpoint, String clientId, String clientSecret) {
		this.accessTokenEndpoint = accessTokenEndpoint;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public TokenStore getTokenStore() {
		return tokenStore;
	}

	public void setTokenStore(TokenStore tokenStore) {
		this.tokenStore = tokenStore;
	}

	public String getAccessTokenEndpoint() {
		return accessTokenEndpoint;
	}

	public void setAccessTokenEndpoint(String accessTokenEndpoint) {
		this.accessTokenEndpoint = accessTokenEndpoint;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public String fetchAccessToken() {
		Token token = tokenStore.getToken();
		if (token != null && !token.isExpired())
			return token.getAccessToken();
		synchronized (this) {
			if (token != null && token.isExpired()) {
				Map<String, String> params = new HashMap<>();
				params.put("grant_type", "refresh_token");
				params.put("client_id", getClientId());
				params.put("client_secret", getClientSecret());
				params.put("refresh_token", token.getRefreshToken());
				try {
					token = internalRestTemplate.getForObject(
							accessTokenEndpoint
									+ "?grant_type=refresh_token&refresh_token={refresh_token}&client_id={client_id}&client_secret={client_secret}",
							Token.class, params);
				} catch (HttpClientErrorException e) {
					if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
							&& e.getResponseBodyAsString().toLowerCase().contains("invalid_token")) {
						token = null;
						tokenStore.setToken(null);
					} else {
						throw e;
					}
				}
			}
			token = tokenStore.getToken();
			if (token == null) {
				Map<String, String> params = new HashMap<>();
				params.put("grant_type", "client_credential");
				params.put("client_id", getClientId());
				params.put("client_secret", getClientSecret());
				token = internalRestTemplate.getForObject(
						accessTokenEndpoint
								+ "?grant_type=client_credential&client_id={client_id}&client_secret={client_secret}",
						Token.class, params);
				tokenStore.setToken(token);
			}
		}
		return token.getAccessToken();
	}

}
