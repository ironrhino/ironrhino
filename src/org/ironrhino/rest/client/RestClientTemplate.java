package org.ironrhino.rest.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.http.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

class RestClientTemplate extends RestTemplate {

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
				ClientHttpResponse response = execution.execute(request, body);
				if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
					try (BufferedReader br = new BufferedReader(
							new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
						String text = br.lines().collect(Collectors.joining("\n")).toLowerCase(Locale.ROOT);
						if (text.contains("invalid_token")) {
							client.getTokenStore().setToken(client.getTokenStoreKey(), null);
						} else if (text.contains("expired_token")) {
							client.getTokenStore().setToken(client.getTokenStoreKey(), null);
						}
						request.getHeaders().set("Authorization", client.getAuthorizationHeader());
						return execution.execute(request, body);
					}
				}
				return response;
			}
		});
	}

}
