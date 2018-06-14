package org.ironrhino.core.spring.http.client;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.MDC;

public class HttpComponentsClientHttpRequestFactory
		extends org.springframework.http.client.HttpComponentsClientHttpRequestFactory {

	public static final int DEFAULT_CONNECTTIMEOUT = 5000;

	public static final int DEFAULT_READTIMEOUT = 10000;

	public HttpComponentsClientHttpRequestFactory() {
		this(false);
	}

	public HttpComponentsClientHttpRequestFactory(boolean trustAllHosts) {
		HttpClientBuilder builder = HttpClients.custom().disableAuthCaching().disableConnectionState()
				.disableCookieManagement().setMaxConnPerRoute(100).setMaxConnTotal(100)
				.setRetryHandler((ex, executionCount, context) -> {
					if (executionCount > 3)
						return false;
					if (ex instanceof NoHttpResponseException)
						return true;
					return false;
				});
		if (trustAllHosts) {
			try {
				SSLContextBuilder sbuilder = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> {
					return true;
				});
				builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sbuilder.build()));
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
				e.printStackTrace();
			}
		}
		setHttpClient(builder.build());
		setConnectTimeout(DEFAULT_CONNECTTIMEOUT);
		setReadTimeout(DEFAULT_READTIMEOUT);
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		String requestChain = MDC.get(AccessFilter.MDC_KEY_REQUEST_CHAIN);
		if (requestChain != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_CHAIN, requestChain);
		request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_FROM, AppInfo.getInstanceId(true));
		super.postProcessHttpRequest(request);
	}

}
