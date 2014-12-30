package org.ironrhino.core.spring.http.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class HttpComponentsClientHttpRequestFactory extends
		org.springframework.http.client.HttpComponentsClientHttpRequestFactory {

	public HttpComponentsClientHttpRequestFactory() {
		HttpClient httpClient = HttpClients.custom().disableAuthCaching()
				.disableAutomaticRetries().disableConnectionState()
				.disableCookieManagement().setMaxConnPerRoute(1000)
				.setMaxConnTotal(1000).build();
		setHttpClient(httpClient);
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
	}

}
