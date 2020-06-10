package org.ironrhino.core.spring.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

public class TrustAllHostsSimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

	private final boolean trustAllHosts;

	public TrustAllHostsSimpleClientHttpRequestFactory(boolean trustAllHosts) {
		this.trustAllHosts = trustAllHosts;
	}

	@Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (trustAllHosts && connection instanceof HttpsURLConnection) {
			((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
			((HttpsURLConnection) connection).setSSLSocketFactory(initSSLContext().getSocketFactory());
		}
		super.prepareConnection(connection, httpMethod);
	}

	private SSLContext initSSLContext() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };
			SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			return sslContext;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}