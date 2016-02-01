package org.ironrhino.core.remoting.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Map;

import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.servlet.AccessFilter;
import org.slf4j.MDC;

public class SimpleHttpInvokerRequestExecutor
		extends org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor {

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
}