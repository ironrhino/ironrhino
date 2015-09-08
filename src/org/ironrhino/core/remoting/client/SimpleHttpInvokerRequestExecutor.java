package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class SimpleHttpInvokerRequestExecutor
		extends org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor {

	@Override
	protected void prepareConnection(HttpURLConnection con, int contentLength) throws IOException {
		String requestId = MDC.get(AccessFilter.MDC_KEY_REQUEST_ID);
		if (requestId != null)
			con.addRequestProperty(AccessFilter.HTTP_HEADER_REQUEST_ID, requestId);
		super.prepareConnection(con, contentLength);
	}
}