package org.ironrhino.core.tracing;

import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpMessage;

import io.opentracing.propagation.TextMap;

public final class HttpComponentsHttpMessageTextMap implements TextMap {

	private final HttpMessage message;

	public HttpComponentsHttpMessageTextMap(HttpMessage message) {
		this.message = message;
	}

	@Override
	public Iterator<Map.Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("Should only be used with Tracer.inject()");
	}

	@Override
	public void put(String key, String value) {
		message.setHeader(key, value);
	}
}