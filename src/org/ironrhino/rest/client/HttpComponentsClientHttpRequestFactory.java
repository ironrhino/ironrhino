package org.ironrhino.rest.client;

import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.MDC;

public class HttpComponentsClientHttpRequestFactory extends
		org.springframework.http.client.HttpComponentsClientHttpRequestFactory {

	public static final String HTTP_HEADER_REQUEST_ID = "X-Request-Id";

	public static final String MDC_KEY_REQUEST_ID = "requestId";

	public static final int DEFAULT_CONNECTTIMEOUT = 5000;

	public static final int DEFAULT_READTIMEOUT = 5000;

	private Client client;

	private int connectTimeout = DEFAULT_CONNECTTIMEOUT;

	private int readTimeout = DEFAULT_READTIMEOUT;

	public HttpComponentsClientHttpRequestFactory(Client client) {
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
				60, TimeUnit.SECONDS);
		connManager.setDefaultMaxPerRoute(1000);
		connManager.setMaxTotal(1000);
		HttpClient httpClient = HttpClientBuilder.create()
				.setConnectionManager(connManager)
				.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
				.disableAuthCaching().disableAutomaticRetries()
				.disableConnectionState().disableCookieManagement().build();
		setHttpClient(httpClient);
		this.client = client;
		super.setConnectTimeout(connectTimeout);
		super.setReadTimeout(readTimeout);
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		super.setConnectTimeout(connectTimeout);
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		super.setReadTimeout(readTimeout);
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		String requestId = MDC.get(MDC_KEY_REQUEST_ID);
		if (requestId != null)
			request.addHeader(HTTP_HEADER_REQUEST_ID, requestId);
		if (client != null)
			request.addHeader("Authorization",
					"Bearer " + client.fetchAccessToken());
	}

}
