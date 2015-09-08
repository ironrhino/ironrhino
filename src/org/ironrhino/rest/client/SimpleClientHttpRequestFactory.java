package org.ironrhino.rest.client;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.slf4j.MDC;

public class SimpleClientHttpRequestFactory extends org.springframework.http.client.SimpleClientHttpRequestFactory {

	public static final String HTTP_HEADER_REQUEST_ID = "X-Request-Id";

	public static final String MDC_KEY_REQUEST_ID = "requestId";

	public static final int DEFAULT_CONNECTTIMEOUT = 5000;

	public static final int DEFAULT_READTIMEOUT = 5000;

	private RestClient client;

	private int connectTimeout = DEFAULT_CONNECTTIMEOUT;

	private int readTimeout = DEFAULT_READTIMEOUT;

	public SimpleClientHttpRequestFactory(RestClient client) {
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
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		super.prepareConnection(connection, httpMethod);
		String requestId = MDC.get(MDC_KEY_REQUEST_ID);
		if (requestId != null)
			connection.addRequestProperty(HTTP_HEADER_REQUEST_ID, requestId);
		if (client != null)
			connection.addRequestProperty("Authorization", "Bearer " + client.fetchAccessToken());

	}

}
