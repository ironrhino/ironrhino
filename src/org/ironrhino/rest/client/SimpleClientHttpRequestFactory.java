package org.ironrhino.rest.client;

import java.io.IOException;
import java.net.HttpURLConnection;

import lombok.Getter;

public class SimpleClientHttpRequestFactory extends org.springframework.http.client.SimpleClientHttpRequestFactory {

	public static final int DEFAULT_CONNECTTIMEOUT = 5000;

	public static final int DEFAULT_READTIMEOUT = 10000;

	private RestClient client;

	@Getter
	private int connectTimeout = DEFAULT_CONNECTTIMEOUT;

	@Getter
	private int readTimeout = DEFAULT_READTIMEOUT;

	public SimpleClientHttpRequestFactory(RestClient client) {
		this.client = client;
		super.setConnectTimeout(connectTimeout);
		super.setReadTimeout(readTimeout);
	}

	@Override
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		super.setConnectTimeout(connectTimeout);
	}

	@Override
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		super.setReadTimeout(readTimeout);
	}

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		super.prepareConnection(connection, httpMethod);
		if (client != null)
			connection.addRequestProperty("Authorization", client.getAuthorizationHeader());

	}

}
