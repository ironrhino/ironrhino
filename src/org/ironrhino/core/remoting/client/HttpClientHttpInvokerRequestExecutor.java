package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
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

public class HttpClientHttpInvokerRequestExecutor extends
		AbstractHttpInvokerRequestExecutor {

	private static final int DEFAULT_TIMEOUT = 5000;

	private PoolingHttpClientConnectionManager connectionManager;

	private CloseableHttpClient httpClient;

	@PostConstruct
	public void init() {
		connectionManager = new PoolingHttpClientConnectionManager(60,
				TimeUnit.SECONDS);
		connectionManager.setDefaultMaxPerRoute(5);
		connectionManager.setMaxTotal(100);
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(DEFAULT_TIMEOUT).build();
		httpClient = HttpClientBuilder.create()
				.setConnectionManager(connectionManager)
				.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
				.setDefaultRequestConfig(requestConfig).build();
	}

	@Override
	protected RemoteInvocationResult doExecuteRequest(
			HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {
		HttpPost postMethod = new HttpPost(config.getServiceUrl());
		postMethod.setHeader(HTTP_HEADER_CONTENT_TYPE, getContentType());
		postMethod.setHeader(HTTP_HEADER_CONTENT_LENGTH,
				Integer.toString(baos.size()));
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
		HttpResponse rsp = httpClient.execute(postMethod);
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
			postMethod.releaseConnection();
		}

	}

	@PreDestroy
	public void destroy() {
		try {
			connectionManager.shutdown();
			httpClient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
