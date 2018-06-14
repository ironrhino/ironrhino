package org.ironrhino.rest.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.http.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

import lombok.Getter;
import lombok.Setter;

class RestClientTemplate extends RestTemplate {

	private RestClient client;

	@Getter
	@Setter
	private int maxAttempts = 2;

	public RestClientTemplate(RestClient client) {
		super(new HttpComponentsClientHttpRequestFactory(client));
		this.client = client;
		super.init();
	}

	@Override
	protected <T> T doExecute(URI uri, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {
		if (uri.getHost() == null && StringUtils.isNotBlank(client.getApiBaseUrl())) {
			String apiBaseUrl = client.getApiBaseUrl();
			try {
				uri = new URI(apiBaseUrl + uri.toString());
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("apiBaseUrl " + apiBaseUrl + " is not valid uri");
			}
		}
		return doExecute(uri, method, requestCallback, responseExtractor, maxAttempts);
	}

	protected <T> T doExecute(URI uri, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, int attempts) throws RestClientException {
		try {
			return super.doExecute(uri, method, requestCallback, responseExtractor);
		} catch (ResourceAccessException e) {
			if (--attempts < 1)
				throw e;
			return doExecute(uri, method, requestCallback, responseExtractor, attempts);
		} catch (HttpClientErrorException e) {
			logger.error(e.getResponseBodyAsString(), e);
			if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
				String response = e.getResponseBodyAsString().toLowerCase(Locale.ROOT);
				if (response.contains("invalid_token")) {
					client.getTokenStore().setToken(client.getTokenStoreKey(), null);
				} else if (response.contains("expired_token")) {
					client.getTokenStore().setToken(client.getTokenStoreKey(), null);
				}
				if (--attempts < 1)
					throw e;
				return doExecute(uri, method, requestCallback, responseExtractor, attempts);
			}
			throw e;
		}
	}

}
