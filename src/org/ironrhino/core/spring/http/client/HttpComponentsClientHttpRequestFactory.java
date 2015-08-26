package org.ironrhino.core.spring.http.client;

import java.io.IOException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class HttpComponentsClientHttpRequestFactory
		extends org.springframework.http.client.HttpComponentsClientHttpRequestFactory {

	public HttpComponentsClientHttpRequestFactory() {
		HttpClient httpClient = HttpClients.custom().disableAuthCaching().disableConnectionState()
				.disableCookieManagement().setMaxConnPerRoute(1000).setMaxConnTotal(1000)
				.setRetryHandler((IOException ex, int executionCount, HttpContext context) -> {
					if (executionCount > 3)
						return false;
					if (ex instanceof NoHttpResponseException)
						return true;
					return false;
				}).build();
		setHttpClient(httpClient);
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
	}

}
