package org.ironrhino.core.spring.http.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };
			try {
				SSLContext sc = SSLContext.getInstance("TLS");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				SSLSocketFactory ssf = sc.getSocketFactory();
				https.setSSLSocketFactory(ssf);
			} catch (KeyManagementException | NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			connection.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
	}
}
