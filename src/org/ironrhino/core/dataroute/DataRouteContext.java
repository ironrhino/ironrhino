package org.ironrhino.core.dataroute;

import java.util.ArrayDeque;
import java.util.Deque;

public class DataRouteContext {

	public static final int DEFAULT_DATASOURCE_WEIGHT = 1;

	private static ThreadLocal<Deque<Boolean>> readonly = new ThreadLocal<>();

	private static ThreadLocal<String> routingKey = new ThreadLocal<>();

	private static ThreadLocal<String> routerName = new ThreadLocal<>();

	private static ThreadLocal<String> nodeName = new ThreadLocal<>();

	static void pushReadonly(Boolean bl) {
		Deque<Boolean> deque = readonly.get();
		if (deque == null) {
			deque = new ArrayDeque<>();
			readonly.set(deque);
		}
		deque.push(bl);
	}

	static Boolean popReadonly() {
		Deque<Boolean> deque = readonly.get();
		if (deque == null) {
			throw new IllegalStateException("readonly should be present");
		}
		return deque.pop();
	}

	static boolean hasReadonly() {
		Deque<Boolean> deque = readonly.get();
		return deque != null && deque.size() > 0;
	}

	static boolean isReadonly() {
		Deque<Boolean> deque = readonly.get();
		if (deque == null || deque.size() == 0)
			return false;
		return deque.peek();
	}

	public static void setNodeName(String s) {
		nodeName.set(s);
	}

	static String getNodeName() {
		String s = nodeName.get();
		nodeName.remove();
		return s;
	}

	public static void setRoutingKey(String s) {
		routingKey.set(s);
	}

	static String getRoutingKey() {
		String s = routingKey.get();
		routingKey.remove();
		return s;
	}

	public static void setRouterName(String s) {
		routerName.set(s);
	}

	static String getRouterName() {
		String s = routerName.get();
		routerName.remove();
		return s;
	}

}