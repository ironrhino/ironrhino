package org.ironrhino.rest.client;

import java.net.URI;

import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.client.token.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;

public class RestTemplate extends org.springframework.web.client.RestTemplate {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private RestClient client;

	private int maxAttempts = 2;

	public RestTemplate(RestClient client) {
		super();
		this.client = client;
		setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
		MappingJackson2HttpMessageConverter jackson2 = null;
		for (HttpMessageConverter<?> hmc : getMessageConverters()) {
			if (hmc instanceof MappingJackson2HttpMessageConverter) {
				jackson2 = (MappingJackson2HttpMessageConverter) hmc;
				break;
			}
		}
		if (jackson2 == null) {
			jackson2 = new MappingJackson2HttpMessageConverter();
			getMessageConverters().add(jackson2);
		}
		jackson2.setObjectMapper(JsonUtils.createNewObjectMapper());
	}

	public int getConnectTimeout() {
		SimpleClientHttpRequestFactory cf = (SimpleClientHttpRequestFactory) getRequestFactory();
		return cf.getConnectTimeout();
	}

	public void setConnectTimeout(int connectTimeout) {
		SimpleClientHttpRequestFactory cf = (SimpleClientHttpRequestFactory) getRequestFactory();
		cf.setConnectTimeout(connectTimeout);
	}

	public int getReadTimeout() {
		SimpleClientHttpRequestFactory cf = (SimpleClientHttpRequestFactory) getRequestFactory();
		return cf.getReadTimeout();
	}

	public void setReadTimeout(int readTimeout) {
		SimpleClientHttpRequestFactory cf = (SimpleClientHttpRequestFactory) getRequestFactory();
		cf.setReadTimeout(readTimeout);
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	@Override
	protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor) throws RestClientException {
		return doExecute(url, method, requestCallback, responseExtractor, maxAttempts);
	}

	protected <T> T doExecute(URI url, HttpMethod method, RequestCallback requestCallback,
			ResponseExtractor<T> responseExtractor, int attempts) throws RestClientException {
		try {
			T result = super.doExecute(url, method, requestCallback, responseExtractor);
			return result;
		} catch (ResourceAccessException e) {
			logger.error(e.getMessage(), e);
			if (--attempts < 1)
				throw e;
			return doExecute(url, method, requestCallback, responseExtractor, attempts);
		} catch (HttpClientErrorException e) {
			logger.error(e.getResponseBodyAsString(), e);
			if (e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
				String response = e.getResponseBodyAsString().toLowerCase();
				Token token = client.getTokenStore().getToken();
				if (response.contains("invalid_token")) {
					client.getTokenStore().setToken(null);
				} else if (response.contains("expired_token")) {
					if (token != null) {
						token.setExpired(true);
						client.getTokenStore().setToken(token);
					}
				}
				if (--attempts < 1)
					throw e;
				return doExecute(url, method, requestCallback, responseExtractor, attempts);
			}
			throw e;
		}
	}

}
