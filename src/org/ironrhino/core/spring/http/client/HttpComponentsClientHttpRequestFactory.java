package org.ironrhino.core.spring.http.client;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class HttpComponentsClientHttpRequestFactory
		extends org.springframework.http.client.HttpComponentsClientHttpRequestFactory {

	public HttpComponentsClientHttpRequestFactory() {
		setHttpClient(builder().build());
	}

	public HttpComponentsClientHttpRequestFactory(boolean trustAllHosts) {
		HttpClientBuilder builder = builder();
		if (trustAllHosts) {
			try {
				SSLContextBuilder sbuilder = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
					@Override
					public boolean isTrusted(final X509Certificate[] chain, final String authType)
							throws CertificateException {
						return true;
					}
				});
				builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sbuilder.build()));
			} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
				e.printStackTrace();
			}
		}
		setHttpClient(builder.build());
	}

	private HttpClientBuilder builder() {
		return HttpClients.custom().disableAuthCaching().disableConnectionState().disableCookieManagement()
				.setMaxConnPerRoute(1000).setMaxConnTotal(1000).setRetryHandler((ex, executionCount, context) -> {
					if (executionCount > 3)
						return false;
					if (ex instanceof NoHttpResponseException)
						return true;
					return false;
				});
	}

	@Override
	protected void postProcessHttpRequest(HttpUriRequest request) {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		String requestChain = MDC.get(AccessFilter.MDC_KEY_REQUEST_CHAIN);
		if (requestChain != null)
			request.addHeader(AccessFilter.HTTP_HEADER_REQUEST_CHAIN, requestChain);
	}

}
