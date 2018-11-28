package org.ironrhino.core.tracing;

import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.Map;

import io.opentracing.propagation.TextMap;

public final class HttpURLConnectionTextMap implements TextMap {

	private final HttpURLConnection connection;

	public HttpURLConnectionTextMap(HttpURLConnection connection) {
		this.connection = connection;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("Should only be used with Tracer.inject()");
	}

	@Override
	public void put(String key, String value) {
		connection.addRequestProperty(key, value);
	}
}