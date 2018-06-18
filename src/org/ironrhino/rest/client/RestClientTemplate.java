package org.ironrhino.rest.client;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.http.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;

class RestClientTemplate extends RestTemplate {

	@Getter
	@Setter
	private int maxAttempts = 2;

	public RestClientTemplate(RestClient client) {
		super();
		Assert.notNull(client, "client must not be null");
		this.getInterceptors().add(new ClientHttpRequestInterceptor() {
			@Override
			public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
					throws IOException {
				URI uri = request.getURI();
				if (uri.getHost() == null && StringUtils.isNotBlank(client.getApiBaseUrl())) {
					HttpRequest origin = request;
					HttpRequest req = new HttpRequest() {
						@Override
						public HttpHeaders getHeaders() {
							return origin.getHeaders();
						}

						@Override
						public String getMethodValue() {
							return origin.getMethodValue();
						}

						@Override
						public URI getURI() {
							String apiBaseUrl = client.getApiBaseUrl();
							try {
								return new URI(apiBaseUrl + uri.toString());
							} catch (URISyntaxException e) {
								throw new IllegalArgumentException("apiBaseUrl " + apiBaseUrl + " is not valid uri");
							}
						}
					};
					request = req;
				}
				request.getHeaders().set("Authorization", client.getAuthorizationHeader());
				int n = maxAttempts;
				if (n < 0 || n > 10)
					n = 1;
				IOException ex = null;
				for (int i = 0; i < n; i++) {
					try {
						ClientHttpResponse response = execution.execute(request, body);
						if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
							String text = String.join("", IOUtils.readLines(response.getBody(), StandardCharsets.UTF_8))
									.toLowerCase(Locale.ROOT);
							if (text.contains("invalid_token")) {
								client.getTokenStore().setToken(client.getTokenStoreKey(), null);
							} else if (text.contains("expired_token")) {
								client.getTokenStore().setToken(client.getTokenStoreKey(), null);
							}
							request.getHeaders().set("Authorization", client.getAuthorizationHeader());
							return execution.execute(request, body);
						}
						return response;
					} catch (SocketTimeoutException e) {
						ex = e;
					}
				}
				throw ex;
			}
		});
	}

}
