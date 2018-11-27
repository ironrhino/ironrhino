package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.MDC;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.remoting.support.RemoteInvocationResult;

import lombok.Getter;
import lombok.Setter;

public class HttpComponentsHttpInvokerRequestExecutor extends HttpInvokerRequestExecutor {

	private CloseableHttpClient httpClient;

	@Getter
	@Setter
	private long connectionTimeToLiveInSeconds = 60;

	@PostConstruct
	public void init() {
		httpClient = HttpClients.custom().disableAuthCaching().disableAutomaticRetries().disableConnectionState()
				.disableCookieManagement().setConnectionTimeToLive(connectionTimeToLiveInSeconds, TimeUnit.SECONDS)
				.build();
	}

	@PreDestroy
	public void destroy() {
		if (httpClient != null)
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	protected RemoteInvocationResult doExecuteRequest(String serviceUrl, MethodInvocation methodInvocation,
			ByteArrayOutputStream baos) throws IOException {
		HttpPost postMethod = new HttpPost(serviceUrl);
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			postMethod.addHeader(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		String requestChain = MDC.get(AccessFilter.MDC_KEY_REQUEST_CHAIN);
		if (requestChain != null)
			postMethod.addHeader(AccessFilter.HTTP_HEADER_REQUEST_CHAIN, requestChain);
		postMethod.addHeader(AccessFilter.HTTP_HEADER_REQUEST_FROM, AppInfo.getInstanceId(true));
		postMethod.setHeader(HTTP_HEADER_CONTENT_TYPE, getSerializer().getContentType());
		postMethod.setHeader(HTTP_HEADER_CONTENT_LENGTH, Integer.toString(baos.size()));
		if (isAcceptGzipEncoding())
			postMethod.setHeader(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		postMethod.setEntity(new ByteArrayEntity(baos.toByteArray()));
		if (getConnectTimeout() >= 0 || getReadTimeout() >= 0) {
			RequestConfig config = RequestConfig.custom().setConnectTimeout(getConnectTimeout())
					.setSocketTimeout(getReadTimeout()).build();
			postMethod.setConfig(config);
		}
		CloseableHttpResponse rsp = httpClient.execute(postMethod);
		try {
			StatusLine sl = rsp.getStatusLine();
			if (sl.getStatusCode() == RemotingContext.SC_SERIALIZATION_FAILED) {
				Header h = rsp.getFirstHeader(RemotingContext.HTTP_HEADER_EXCEPTION_MESSAGE);
				throw new SerializationFailedException(h != null ? h.getValue() : "");
			} else if (sl.getStatusCode() >= 300) {
				throw new IOException("Did not receive successful HTTP response: status code = " + sl.getStatusCode()
						+ ", status message = [" + sl.getReasonPhrase() + "]");
			}
			HttpEntity entity = rsp.getEntity();
			InputStream responseBody = entity.getContent();
			Header h = rsp.getFirstHeader(HttpHeaders.CONTENT_TYPE);
			String contentType = h != null ? h.getValue() : null;
			HttpInvokerSerializer serializer = StringUtils.isNotBlank(contentType)
					? HttpInvokerSerializers.ofContentType(contentType)
					: getSerializer();
			return serializer.readRemoteInvocationResult(methodInvocation, responseBody);
		} finally {
			rsp.close();
		}

	}

}
