package org.ironrhino.core.remoting;

import java.util.HashMap;
import java.util.Map;

public class RemotingContext {

	public static final int SC_SERIALIZATION_FAILED = 499;

	public static final String HTTP_HEADER_EXCEPTION_MESSAGE = "X-Exception-Message";

	public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

	public final static String HTTP_HEADER_PREFIX = "x-rc-";

	private static ThreadLocal<Map<String, String>> context = new ThreadLocal<>();

	private static ThreadLocal<String> _from = new ThreadLocal<>();

	public static Map<String, String> getContext() {
		return context.get();
	}

	public static void put(String key, String value) {
		Map<String, String> map = context.get();
		if (map == null) {
			map = new HashMap<>();
			context.set(map);
		}
		map.put(key, value);
	}

	public static String get(String key) {
		Map<String, String> map = context.get();
		if (map == null)
			return null;
		return map.get(key);
	}

	public static void setRequestFrom(String from) {
		_from.set(from);
	}

	public static String getRequestFrom() {
		return _from.get();
	}

	public static void clear() {
		Map<String, String> map = context.get();
		if (map != null)
			map.clear();
		_from.remove();
	}

}