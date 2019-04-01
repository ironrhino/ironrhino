package org.ironrhino.core.spring.http.client;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.JsonUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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
			if (mc instanceof MappingJackson2XmlHttpMessageConverter || mc instanceof StringHttpMessageConverter)
				it.remove();
			else if (mc instanceof MappingJackson2HttpMessageConverter)
				((MappingJackson2HttpMessageConverter) mc).setObjectMapper(JsonUtils.createNewObjectMapper());
		}
		getMessageConverters().add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
		setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
		setReadTimeout(DEFAULT_READ_TIMEOUT);
	}

	@Override
	public void setRequestFactory(ClientHttpRequestFactory requestFactory) {
		super.setRequestFactory(requestFactory);
		this.requestFactory = requestFactory;
		setConnectTimeout(connectTimeout);
		setReadTimeout(readTimeout);
	}

	@Value("${restTemplate.connectTimeout:" + DEFAULT_CONNECT_TIMEOUT + "}")
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
		if (requestFactory instanceof SimpleClientHttpRequestFactory) {
			SimpleClientHttpRequestFactory schrf = (SimpleClientHttpRequestFactory) requestFactory;
			schrf.setConnectTimeout(connectTimeout);
		} else if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
			HttpComponentsClientHttpRequestFactory hccrf = (HttpComponentsClientHttpRequestFactory) requestFactory;
			hccrf.setConnectTimeout(connectTimeout);
		}
	}

	@Value("${restTemplate.readTimeout:" + DEFAULT_READ_TIMEOUT + "}")
	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
		if (requestFactory instanceof SimpleClientHttpRequestFactory) {
			SimpleClientHttpRequestFactory schrf = (SimpleClientHttpRequestFactory) requestFactory;
			schrf.setReadTimeout(readTimeout);
		} else if (requestFactory instanceof HttpComponentsClientHttpRequestFactory) {
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
			Locale locale = LocaleContextHolder.getLocale();
			if (locale != null)
				request.getHeaders().setAcceptLanguageAsLocales(Collections.singletonList(locale));
			return execution.execute(request, body);
		}

	}

	private static class TrustAllHostsClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

		static {
			allowPatchMethod();
		}

		private final boolean trustAllHosts;

		public TrustAllHostsClientHttpRequestFactory(boolean trustAllHosts) {
			this.trustAllHosts = trustAllHosts;
		}

		@Override
		protected void prepareConnection(final HttpURLConnection connection, final String httpMethod)
				throws IOException {
			if (trustAllHosts && connection instanceof HttpsURLConnection) {
				((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
				((HttpsURLConnection) connection).setSSLSocketFactory(initSSLContext().getSocketFactory());
			}
			super.prepareConnection(connection, httpMethod);
		}

		private SSLContext initSSLContext() {
			try {
				TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				} };
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
				return sslContext;
			} catch (final Exception ex) {
				return null;
			}
		}

		private static void allowPatchMethod() {
			try {
				Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
				Field modifiersField = Field.class.getDeclaredField("modifiers");
				modifiersField.setAccessible(true);
				modifiersField.setInt(methodsField, methodsField.getModifiers() & ~Modifier.FINAL);
				methodsField.setAccessible(true);
				String[] oldMethods = (String[]) methodsField.get(null);
				Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
				if (!methodsSet.contains("PATCH")) {
					methodsSet.add("PATCH");
					String[] newMethods = methodsSet.toArray(new String[0]);
					methodsField.set(null, newMethods);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
