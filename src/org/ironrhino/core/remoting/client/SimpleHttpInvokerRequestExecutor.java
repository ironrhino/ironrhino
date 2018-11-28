package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.MDC;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.remoting.support.RemoteInvocationResult;

public class SimpleHttpInvokerRequestExecutor extends HttpInvokerRequestExecutor {

	@Override
	protected RemoteInvocationResult doExecuteRequest(String serviceUrl, MethodInvocation methodInvocation,
			ByteArrayOutputStream baos) throws IOException {
		HttpURLConnection con = (HttpURLConnection) new URL(serviceUrl).openConnection();
		prepareConnection(con, baos.size());
		baos.writeTo(con.getOutputStream());
		validateResponse(con);
		InputStream responseBody = readResponseBody(con);
		String contentType = con.getHeaderField(HttpHeaders.CONTENT_TYPE);
		HttpInvokerSerializer serializer = StringUtils.isNotBlank(contentType)
				? HttpInvokerSerializers.ofContentType(contentType)
				: getSerializer();
		return serializer.readRemoteInvocationResult(methodInvocation, responseBody);
	}

	protected void prepareConnection(HttpURLConnection connection, int contentLength) throws IOException {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			connection.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		String requestChain = MDC.get(AccessFilter.MDC_KEY_REQUEST_CHAIN);
		if (requestChain != null)
			connection.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_CHAIN, requestChain);
		connection.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_FROM, AppInfo.getInstanceId(true));

		Tracing.inject(connection);

		if (getConnectTimeout() >= 0)
			connection.setConnectTimeout(getConnectTimeout());

		if (getReadTimeout() >= 0)
			connection.setReadTimeout(getReadTimeout());

		connection.setDoOutput(true);
		connection.setRequestMethod(HTTP_METHOD_POST);
		connection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, getSerializer().getContentType());
		connection.setRequestProperty(HTTP_HEADER_CONTENT_LENGTH, Integer.toString(contentLength));

		if (isAcceptGzipEncoding()) {
			connection.setRequestProperty(HTTP_HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
		}
	}

	protected void validateResponse(HttpURLConnection con) throws IOException {
		if (con.getResponseCode() == RemotingContext.SC_SERIALIZATION_FAILED)
			throw new SerializationFailedException(con.getHeaderField(RemotingContext.HTTP_HEADER_EXCEPTION_MESSAGE));
		if (con.getResponseCode() >= 300) {
			throw new IOException("Did not receive successful HTTP response: status code = " + con.getResponseCode()
					+ ", status message = [" + con.getResponseMessage() + "]");
		}
	}

	protected InputStream readResponseBody(HttpURLConnection con) throws IOException {
		String encodingHeader = con.getHeaderField(HTTP_HEADER_CONTENT_ENCODING);
		if ((encodingHeader != null && encodingHeader.toLowerCase(Locale.ROOT).contains(ENCODING_GZIP))) {
			return new GZIPInputStream(con.getInputStream());
		} else {
			return con.getInputStream();
		}
	}

}