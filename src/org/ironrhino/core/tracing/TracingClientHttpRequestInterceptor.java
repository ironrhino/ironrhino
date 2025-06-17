package org.ironrhino.core.tracing;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		return Tracing.executeCheckedCallable("RestTemplate.execute", () -> {
			TextMapPropagators.inject(request);
			ClientHttpResponse response = execution.execute(request, body);
			Tracing.setTags("http.status", response.getStatusCode().value());
			return response;
		}, "span.kind", "client", "http.method", request.getMethodValue(), "http.url", request.getURI().toString());
	}

}
