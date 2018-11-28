package org.ironrhino.core.tracing;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import io.jaegertracing.internal.Constants;
import io.opentracing.propagation.TextMap;

public final class HttpServletRequestTextMap implements TextMap {

	private final Map<String, String> map;

	public HttpServletRequestTextMap(HttpServletRequest request) {
		map = new HashMap<>();
		Enumeration<String> en = request.getHeaderNames();
		while (en.hasMoreElements()) {
			String name = en.nextElement();
			String lowerCaseName = name.toLowerCase(); // Jetty return X-Trace-Id
			if (lowerCaseName.equals(TracingConfiguration.SPAN_CONTEXT_KEY)
					|| lowerCaseName.startsWith(TracingConfiguration.BAGGAG_EPREFIX)
					|| lowerCaseName.equals(Constants.DEBUG_ID_HEADER_KEY))
				map.put(name, request.getHeader(name));
		}
		String debugId = request.getParameter(Constants.DEBUG_ID_HEADER_KEY);
		if (debugId != null)
			map.put(Constants.DEBUG_ID_HEADER_KEY, debugId);
		String baggage = request.getParameter(Constants.BAGGAGE_HEADER_KEY);
		if (baggage != null)
			map.put(Constants.BAGGAGE_HEADER_KEY, baggage);
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		return map.entrySet().iterator();
	}

	@Override
	public void put(String key, String value) {
		throw new UnsupportedOperationException("Should only be used with Tracer.extract()");
	}
}