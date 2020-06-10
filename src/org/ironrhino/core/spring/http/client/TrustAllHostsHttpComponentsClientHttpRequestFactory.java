package org.ironrhino.core.spring.http.client;

import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

class TrustAllHostsHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

	public TrustAllHostsHttpComponentsClientHttpRequestFactory(boolean trustAllHosts) {
		HttpClientBuilder builder = HttpClients.custom().useSystemProperties().disableAuthCaching()
				.disableConnectionState().disableCookieManagement().setConnectionTimeToLive(60, TimeUnit.SECONDS)
				.setRetryHandler((e, executionCount, httpCtx) -> executionCount < 3
						&& (e instanceof NoHttpResponseException || e instanceof UnknownHostException));
		if (trustAllHosts) {
			try {
				SSLContextBuilder sbuilder = SSLContexts.custom().loadTrustMaterial(null, (chain, authType) -> {
					return true;
				});
				builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sbuilder.build()));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		setHttpClient(builder.build());
	}

}