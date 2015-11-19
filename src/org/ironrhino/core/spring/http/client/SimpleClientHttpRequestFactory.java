package org.ironrhino.core.spring.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HttpsURLConnection;

import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class SimpleClientHttpRequestFactory extends org.springframework.http.client.SimpleClientHttpRequestFactory {

	private boolean trustAllHosts;

	public SimpleClientHttpRequestFactory() {
		super();
	}

	public SimpleClientHttpRequestFactory(boolean trustAllHosts) {
		super();
		this.trustAllHosts = trustAllHosts;
	}

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		super.prepareConnection(connection, httpMethod);
		if (trustAllHosts && connection instanceof HttpsURLConnection) {
			HttpsURLConnection https = (HttpsURLConnection) connection;
			https.setHostnameVerifier((name, session) -> true);
		}
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			connection.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
	}
}
