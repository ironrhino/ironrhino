package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;

import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.SerializationType;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

public class SimpleHttpInvokerRequestExecutor
		extends org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor {

	private SerializationType serializationType;

	public SimpleHttpInvokerRequestExecutor(SerializationType serializationType) {
		this.serializationType = serializationType;
	}

	@Override
	protected void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			con.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		Map<String, String> map = RemotingContext.getContext();
		if (map != null) {
			for (Map.Entry<String, String> entry : map.entrySet())
				con.addRequestProperty(RemotingContext.HTTP_HEADER_PREFIX + URLEncoder.encode(entry.getKey(), "UTF-8"),
						URLEncoder.encode(entry.getValue(), "UTF-8"));
		}
		super.prepareConnection(con, contentLength);
	}

	@Override
	public String getContentType() {
		return serializationType.getContentType();
	}

	@Override
	protected void writeRemoteInvocation(RemoteInvocation invocation, OutputStream os) throws IOException {
		if (serializationType != SerializationType.JAVA)
			serializationType.writeRemoteInvocation(invocation, decorateOutputStream(os));
		else
			super.writeRemoteInvocation(invocation, os);
	}

	@Override
	protected RemoteInvocationResult readRemoteInvocationResult(InputStream is, String codebaseUrl)
			throws IOException, ClassNotFoundException {
		if (serializationType != SerializationType.JAVA)
			return serializationType.readRemoteInvocationResult(decorateInputStream(is));
		else
			return super.readRemoteInvocationResult(is, codebaseUrl);
	}

}