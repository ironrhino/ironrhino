package org.ironrhino.rest.client;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.rest.client.token.DefaultToken;
import org.ironrhino.rest.client.token.DefaultTokenStore;
import org.ironrhino.rest.client.token.Token;
import org.ironrhino.rest.client.token.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class RestClient implements BeanNameAware {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected String accessTokenEndpoint;

	protected String clientId;

	protected String clientSecret;

	protected String scope;

	protected String grantType = "client_credentials";

	protected RestTemplate restTemplate = new RestClientTemplate(this);

	protected RestTemplate internalRestTemplate = new RestTemplate();

	protected TokenStore tokenStore = new DefaultTokenStore();

	protected Class<? extends Token> tokenClass = DefaultToken.class;

	protected String beanName;

	public RestClient() {

	}

	public RestClient(String accessTokenEndpoint, String clientId, String clientSecret) {
		this.accessTokenEndpoint = accessTokenEndpoint;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public RestClient(String accessTokenEndpoint, String clientId, String clientSecret, String scope) {
		this(accessTokenEndpoint, clientId, clientSecret);
		this.scope = scope;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
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

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	public String getGrantType() {
		return grantType;
	}

	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}

	public Class<? extends Token> getTokenClass() {
		return tokenClass;
	}

	public void setTokenClass(Class<? extends Token> tokenClass) {
		if (tokenClass != null)
			this.tokenClass = tokenClass;
	}

	public RestTemplate getRestTemplate() {
		return restTemplate;
	}

	public String fetchAccessToken() {
		return fetchToken().getAccessToken();
	}

	public String getTokenStoreKey() {
		return beanName == null ? getClientId() : beanName + ":" + getClientId();
	}

	protected Token fetchToken() {
		String tokenStoreKey = getTokenStoreKey();
		Token token = tokenStore.getToken(tokenStoreKey);
		if (token == null || token.isExpired()) {
			synchronized (this) {
				token = tokenStore.getToken(tokenStoreKey);
				if (token == null || token.isExpired()) {
					token = tryRefreshToken(token);
					tokenStore.setToken(tokenStoreKey, token);
				}
			}
		}
		return token;
	}

	private Token tryRefreshToken(Token token) {
		if (token != null && StringUtils.isNotBlank(token.getRefreshToken())) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("grant_type", "refresh_token");
			params.add("client_id", getClientId());
			params.add("client_secret", getClientSecret());
			params.add("refresh_token", token.getRefreshToken());
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
			try {
				token = internalRestTemplate.postForEntity(accessTokenEndpoint, request, getTokenClass()).getBody();
			} catch (HttpClientErrorException e) {
				if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)
						|| e.getResponseBodyAsString().toLowerCase(Locale.ROOT).contains("invalid_token")) {
					token = requestToken();
				} else {
					throw e;
				}
			}
		} else {
			token = requestToken();
		}
		return token;
	}

	private Token requestToken() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", getGrantType());
		params.add("client_id", getClientId());
		params.add("client_secret", getClientSecret());
		if (getScope() != null)
			params.add("scope", getScope());
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
		try {
			return internalRestTemplate.postForEntity(accessTokenEndpoint, request, getTokenClass()).getBody();
		} catch (HttpClientErrorException e) {
			logger.error(e.getResponseBodyAsString());
			throw e;
		}
	}

	protected String getAuthorizationHeader() {
		return "Bearer " + fetchAccessToken();
	}

}
