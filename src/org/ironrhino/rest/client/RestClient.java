package org.ironrhino.rest.client;

import java.util.Iterator;
import java.util.Locale;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.rest.client.token.CacheBasedTokenStore;
import org.ironrhino.rest.client.token.DefaultToken;
import org.ironrhino.rest.client.token.DefaultTokenStore;
import org.ironrhino.rest.client.token.Token;
import org.ironrhino.rest.client.token.TokenStore;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestClient implements BeanNameAware {

	@Getter
	@Setter
	protected String accessTokenEndpoint;

	@Getter
	@Setter
	protected String clientId;

	@Getter
	@Setter
	protected String clientSecret;

	@Getter
	@Setter
	protected String scope;

	@Getter
	@Setter
	protected String grantType = "client_credentials";

	@Getter
	@Setter
	protected String apiBaseUrl;

	@Getter
	protected RestTemplate restTemplate = new RestClientTemplate(this);

	protected RestTemplate internalRestTemplate = new RestTemplate();

	@Autowired(required = false)
	private CacheManager cacheManager;

	@Getter
	@Setter
	protected TokenStore tokenStore = new DefaultTokenStore();

	@Getter
	@Setter
	protected Class<? extends Token> tokenClass = DefaultToken.class;

	@Setter
	protected String beanName;

	public RestClient() {
		Iterator<HttpMessageConverter<?>> it = internalRestTemplate.getMessageConverters().iterator();
		while (it.hasNext()) {
			if (it.next() instanceof MappingJackson2XmlHttpMessageConverter)
				it.remove();
		}
	}

	public RestClient(String accessTokenEndpoint, String clientId, String clientSecret) {
		this();
		Assert.notNull(accessTokenEndpoint, "accessTokenEndpoint shouldn't be null");
		Assert.notNull(clientId, "clientId shouldn't be null");
		Assert.notNull(clientSecret, "clientSecret shouldn't be null");
		this.accessTokenEndpoint = accessTokenEndpoint;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public RestClient(String apiBaseUrl, String accessTokenEndpoint, String clientId, String clientSecret) {
		this();
		Assert.notNull(apiBaseUrl, "apiBaseUrl shouldn't be null");
		Assert.notNull(accessTokenEndpoint, "accessTokenEndpoint shouldn't be null");
		Assert.notNull(clientId, "clientId shouldn't be null");
		Assert.notNull(clientSecret, "clientSecret shouldn't be null");
		this.apiBaseUrl = apiBaseUrl;
		if (accessTokenEndpoint.indexOf("://") < 0 && apiBaseUrl != null)
			accessTokenEndpoint = apiBaseUrl + accessTokenEndpoint;
		this.accessTokenEndpoint = accessTokenEndpoint;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	@PostConstruct
	public void init() {
		if (cacheManager != null)
			this.tokenStore = new CacheBasedTokenStore(cacheManager);
	}

	public String fetchAccessToken() {
		return fetchToken().getAccessToken();
	}

	protected String getTokenStoreKey() {
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
			log.error(e.getResponseBodyAsString());
			throw e;
		}
	}

	protected String getAuthorizationHeader() {
		return "Bearer " + fetchAccessToken();
	}

}
