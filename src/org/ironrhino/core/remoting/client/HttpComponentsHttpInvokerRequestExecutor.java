package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.StringUtils;

public class HttpComponentsHttpInvokerRequestExecutor extends
		AbstractHttpInvokerRequestExecutor {

	private CloseableHttpClient httpClient;

	private long timeToLive = 60;

	private int defaultMaxPerRoute = 1000;

	private int maxTotal = 1000;

	public long getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	public int getDefaultMaxPerRoute() {
		return defaultMaxPerRoute;
	}

	public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
		this.defaultMaxPerRoute = defaultMaxPerRoute;
	}

	public int getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}

	@PostConstruct
	public void init() {
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(
				timeToLive, TimeUnit.SECONDS);
		connManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		connManager.setMaxTotal(maxTotal);
		httpClient = HttpClientBuilder.create()
				.setConnectionManager(connManager)
				.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
				.disableAuthCaching().disableAutomaticRetries()
				.disableConnectionState().disableCookieManagement()
				.disableRedirectHandling().build();
	}

	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {
		HttpPost postMethod = new HttpPost(config.getServiceUrl());
		postMethod.setHeader(HTTP_HEADER_CONTENT_TYPE, getContentType());
		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null)
				postMethod.setHeader(HTTP_HEADER_ACCEPT_LANGUAGE,
						StringUtils.toLanguageTag(locale));
		}
		if (isAcceptGzipEncoding())
			postMethod.setHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			postMethod
					.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		postMethod.setEntity(new ByteArrayEntity(baos.toByteArray()));
		CloseableHttpResponse rsp = httpClient.execute(postMethod);
		try {
			StatusLine sl = rsp.getStatusLine();
			if (sl.getStatusCode() >= 300) {
				throw new IOException(
						"Did not receive successful HTTP response: status code = "
								+ sl.getStatusCode() + ", status message = ["
								+ sl.getReasonPhrase() + "]");
			}
			HttpEntity entity = rsp.getEntity();
			InputStream responseBody = entity.getContent();
			return readRemoteInvocationResult(responseBody,
					config.getCodebaseUrl());
		} finally {
			rsp.close();
			postMethod.releaseConnection();
		}

	}

	@PreDestroy
	public void destroy() {
		try {
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
