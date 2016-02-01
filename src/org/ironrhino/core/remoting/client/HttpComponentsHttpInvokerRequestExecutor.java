package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.remoting.httpinvoker.AbstractHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.StringUtils;

public class HttpComponentsHttpInvokerRequestExecutor extends AbstractHttpInvokerRequestExecutor {

	private CloseableHttpClient httpClient;

	private long timeToLive = 60;

	private int maxConnPerRoute = 1000;

	private int maxConnTotal = 1000;

	public long getTimeToLive() {
		return timeToLive;
	}

	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	public int getMaxConnPerRoute() {
		return maxConnPerRoute;
	}

	public void setMaxConnPerRoute(int maxConnPerRoute) {
		this.maxConnPerRoute = maxConnPerRoute;
	}

	public int getMaxConnTotal() {
		return maxConnTotal;
	}

	public void setMaxConnTotal(int maxConnTotal) {
		this.maxConnTotal = maxConnTotal;
	}

	@PostConstruct
	public void init() {
		httpClient = HttpClients.custom().disableAuthCaching().disableConnectionState().disableCookieManagement()
				.disableRedirectHandling().setMaxConnPerRoute(maxConnPerRoute).setMaxConnTotal(maxConnTotal)
				.setRetryHandler((ex, executionCount, context) -> {
					if (executionCount > 3)
						return false;
					if (ex instanceof NoHttpResponseException)
						return true;
					return false;
				}).build();
	}

	@Override
	protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException, ClassNotFoundException {
		HttpPost postMethod = new HttpPost(config.getServiceUrl());
		postMethod.setHeader(HTTP_HEADER_CONTENT_TYPE, getContentType());
		LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
		if (localeContext != null) {
			Locale locale = localeContext.getLocale();
			if (locale != null)
				postMethod.setHeader(HTTP_HEADER_ACCEPT_LANGUAGE, StringUtils.toLanguageTag(locale));
		}
		if (isAcceptGzipEncoding())
			postMethod.setHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			postMethod.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		Map<String, String> ctx = RemotingContext.getContext();
		if (ctx != null) {
			for (Map.Entry<String, String> entry : ctx.entrySet())
				postMethod.addHeader(RemotingContext.HTTP_HEADER_PREFIX + URLEncoder.encode(entry.getKey(), "UTF-8"),
						URLEncoder.encode(entry.getValue(), "UTF-8"));
		}
		postMethod.setEntity(new ByteArrayEntity(baos.toByteArray()));
		CloseableHttpResponse rsp = httpClient.execute(postMethod);
		try {
			StatusLine sl = rsp.getStatusLine();
			if (sl.getStatusCode() >= 300) {
				throw new IOException("Did not receive successful HTTP response: status code = " + sl.getStatusCode()
						+ ", status message = [" + sl.getReasonPhrase() + "]");
			}
			HttpEntity entity = rsp.getEntity();
			InputStream responseBody = entity.getContent();
			return readRemoteInvocationResult(responseBody, config.getCodebaseUrl());
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
