package org.ironrhino.rest.client;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;

public class HttpComponentsClientHttpRequestFactory
		extends org.springframework.http.client.HttpComponentsClientHttpRequestFactory {

	public static final int DEFAULT_CONNECTTIMEOUT = 5000;

	public static final int DEFAULT_READTIMEOUT = 5000;

	private RestClient client;

	private int connectTimeout = DEFAULT_CONNECTTIMEOUT;

	private int readTimeout = DEFAULT_READTIMEOUT;

	public HttpComponentsClientHttpRequestFactory(RestClient client) {
		HttpClient httpClient = HttpClients.custom().disableAuthCaching().disableConnectionState()
				.disableCookieManagement().setMaxConnPerRoute(100).setMaxConnTotal(100)
				.setRetryHandler((ex, executionCount, context) -> {
					if (executionCount > 3)
						return false;
					if (ex instanceof NoHttpResponseException)
						return true;
					return false;
				}).build();
		setHttpClient(httpClient);
		this.client = client;
		super.setConnectTimeout(connectTimeout);
		super.setReadTimeout(readTimeout);
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	@Override
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		super.setConnectTimeout(connectTimeout);
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	@Override
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		super.setReadTimeout(readTimeout);
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		if (client != null)
			request.addHeader("Authorization", client.getAuthorizationHeader());
	}

}
