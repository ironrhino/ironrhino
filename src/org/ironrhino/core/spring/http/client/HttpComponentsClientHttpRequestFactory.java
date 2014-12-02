package org.ironrhino.core.spring.http.client;

import org.apache.http.client.methods.HttpUriRequest;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class HttpComponentsClientHttpRequestFactory extends
		org.springframework.http.client.HttpComponentsClientHttpRequestFactory {
	
	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
	}

}
