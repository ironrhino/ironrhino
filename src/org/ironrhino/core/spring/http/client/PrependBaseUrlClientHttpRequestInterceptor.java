package org.ironrhino.core.spring.http.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class PrependBaseUrlClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final Supplier<String> baseUrlSupplier;

	public PrependBaseUrlClientHttpRequestInterceptor(Supplier<String> baseUrlSupplier) {
		this.baseUrlSupplier = baseUrlSupplier;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		URI uri = request.getURI();
		if (uri.getHost() == null) {
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
					String baseUrl = baseUrlSupplier.get();
					try {
						return new URI(baseUrl + uri.toString());
					} catch (URISyntaxException e) {
						throw new IllegalArgumentException("baseUrl " + baseUrl + " is not valid uri");
					}
				}
			};
			request = req;
		}
		return execution.execute(request, body);
	}
}
