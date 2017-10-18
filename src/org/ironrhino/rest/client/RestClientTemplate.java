package org.ironrhino.rest.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;
import lombok.Setter;

class RestClientTemplate extends RestTemplate {

	private RestClient client;

	@Getter
	@Setter
	private int maxAttempts = 2;

	public RestClientTemplate(RestClient client) {
		super();
		this.client = client;
		setRequestFactory(new HttpComponentsClientHttpRequestFactory(client));
		Iterator<HttpMessageConverter<?>> it = getMessageConverters().iterator();
		while (it.hasNext()) {
			HttpMessageConverter<?> mc = it.next();
			if (mc instanceof MappingJackson2XmlHttpMessageConverter)
				it.remove();
			else if (mc instanceof MappingJackson2HttpMessageConverter)
				((MappingJackson2HttpMessageConverter) mc).setObjectMapper(JsonUtils.createNewObjectMapper());
		}
	}

	public void setConnectTimeout(int connectTimeout) {
		ClientHttpRequestFactory chrf = getRequestFactory();
		if (chrf instanceof org.springframework.http.client.SimpleClientHttpRequestFactory) {
			org.springframework.http.client.SimpleClientHttpRequestFactory scrf = (org.springframework.http.client.SimpleClientHttpRequestFactory) chrf;
			scrf.setConnectTimeout(connectTimeout);
		} else if (chrf instanceof org.springframework.http.client.HttpComponentsClientHttpRequestFactory) {
			org.springframework.http.client.HttpComponentsClientHttpRequestFactory hccrf = (org.springframework.http.client.HttpComponentsClientHttpRequestFactory) chrf;
			hccrf.setConnectTimeout(connectTimeout);
		}
	}

	public void setReadTimeout(int readTimeout) {
		ClientHttpRequestFactory chrf = getRequestFactory();
		if (chrf instanceof org.springframework.http.client.SimpleClientHttpRequestFactory) {
			org.springframework.http.client.SimpleClientHttpRequestFactory scrf = (org.springframework.http.client.SimpleClientHttpRequestFactory) chrf;
			scrf.setReadTimeout(readTimeout);
		} else if (chrf instanceof org.springframework.http.client.HttpComponentsClientHttpRequestFactory) {
			org.springframework.http.client.HttpComponentsClientHttpRequestFactory hccrf = (org.springframework.http.client.HttpComponentsClientHttpRequestFactory) chrf;
			hccrf.setReadTimeout(readTimeout);
		}
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
