package org.ironrhino.core.spring.http.client;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;

@Component
public class RestTemplate extends org.springframework.web.client.RestTemplate {

	private final static int DEFAULT_CONNECT_TIMEOUT = 5000;

	private final static int DEFAULT_READ_TIMEOUT = 10000;

	private final static boolean TRUST_ALL_HOSTS = false;

	private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

	private int readTimeout = DEFAULT_READ_TIMEOUT;

	private ClientHttpRequestFactory requestFactory;

	public RestTemplate() {
		this(new TrustAllHostsClientHttpRequestFactory(false));
	}

	public RestTemplate(ClientHttpRequestFactory requestFactory) {
		super();
		this.requestFactory = requestFactory;
		super.setRequestFactory(requestFactory);
		this.getInterceptors().add(new AddHeadersClientHttpRequestInterceptor());
		Iterator<HttpMessageConverter<?>> it = getMessageConverters().iterator();
		while (it.hasNext()) {
			HttpMessageConverter<?> mc = it.next();
			if (mc instanceof MappingJackson2XmlHttpMessageConverter)
				it.remove();
			else if (mc instanceof MappingJackson2HttpMessageConverter)
				((MappingJackson2HttpMessageConverter) mc).setObjectMapper(JsonUtils.createNewObjectMapper());
		}
		setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
		setReadTimeout(DEFAULT_READ_TIMEOUT);
	}

	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		super.setRequestFactory(requestFactory);
		this.requestFactory = requestFactory;
		setConnectTimeout(connectTimeout);
		setReadTimeout(readTimeout);
	}

	@Value("${restTemplate.connectTimeout:" + DEFAULT_CONNECT_TIMEOUT + "}")
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
			HttpComponentsClientHttpRequestFactory hccrf = (HttpComponentsClientHttpRequestFactory) requestFactory;
			hccrf.setConnectTimeout(connectTimeout);
		}
	}

	@Value("${restTemplate.readTimeout:" + DEFAULT_READ_TIMEOUT + "}")
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
			HttpComponentsClientHttpRequestFactory hccrf = (HttpComponentsClientHttpRequestFactory) requestFactory;
			hccrf.setReadTimeout(readTimeout);
		}
	}

	@Value("${restTemplate.trustAllHosts:" + TRUST_ALL_HOSTS + "}")
	public void setTrustAllHosts(boolean trustAllHosts) {
		setRequestFactory(new TrustAllHostsClientHttpRequestFactory(trustAllHosts));
	}

	private static class AddHeadersClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
			if (requestId != null)
				request.getHeaders().set(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
			String requestChain = MDC.get(AccessFilter.MDC_KEY_REQUEST_CHAIN);
			if (requestChain != null)
				request.getHeaders().set(AccessFilter.HTTP_HEADER_REQUEST_CHAIN, requestChain);
			request.getHeaders().set(AccessFilter.HTTP_HEADER_REQUEST_FROM, AppInfo.getInstanceId(true));
			return execution.execute(request, body);
		}

	}

	private static class TrustAllHostsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {

		public TrustAllHostsClientHttpRequestFactory(boolean trustAllHosts) {
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
		}

	}

}
