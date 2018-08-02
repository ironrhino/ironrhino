package org.ironrhino.core.remoting.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.MDC;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.remoting.httpinvoker.HttpInvokerClientConfiguration;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class SimpleHttpInvokerRequestExecutor
		extends org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor {

	private final HttpInvokerSerializer serializer;

	public SimpleHttpInvokerRequestExecutor(HttpInvokerSerializer serializer) {
		this.serializer = serializer;
	}

	@Override
	protected void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			con.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		String requestChain = MDC.get(AccessFilter.MDC_KEY_REQUEST_CHAIN);
		if (requestChain != null)
			con.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_CHAIN, requestChain);
		con.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_FROM, AppInfo.getInstanceId(true));
		super.prepareConnection(con, contentLength);
	}

	@Override
	public String getContentType() {
		return serializer.getContentType();
	}

	@Override
	protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		serializer.writeRemoteInvocation(invocation, decorateOutputStream(os));
	}

	@Override
	protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, String codebaseUrl)
			throws IOException, ClassNotFoundException {
		return serializer.readRemoteInvocationResult(decorateInputStream(is));
	}

	@Override
	protected void validateResponse(HttpInvokerClientConfiguration config, HttpURLConnection con) throws IOException {
		if (con.getResponseCode() == RemotingContext.SC_SERIALIZATION_FAILED)
			throw new SerializationFailedException(con.getHeaderField(RemotingContext.HTTP_HEADER_EXCEPTION_MESSAGE));
		super.validateResponse(config, con);
	}

	@Override
	protected RemoteInvocationResult doExecuteRequest(HttpInvokerClientConfiguration config, ByteArrayOutputStream baos)
			throws IOException {
		HttpURLConnection con = openConnection(config);
		prepareConnection(con, baos.size());
		writeRequestBody(config, con, baos);
		validateResponse(config, con);
		InputStream responseBody = readResponseBody(config, con);
		HttpInvokerSerializer serializer = HttpInvokerSerializers
				.ofContentType(con.getHeaderField(HttpHeaders.CONTENT_TYPE));
		return serializer.readRemoteInvocationResult(decorateInputStream(responseBody));
	}

}