package org.ironrhino.core.dataroute;

import java.util.Deque;
import java.util.LinkedList;

public class DataRouteContext {

	public static final int DEFAULT_DATASOURCE_WEIGHT = 1;

	private static ThreadLocal<Deque<Boolean>> readonly = new ThreadLocal<>();

	private static ThreadLocal<String> routingKey = new ThreadLocal<>();

	private static ThreadLocal<String> routerName = new ThreadLocal<>();

	private static ThreadLocal<String> nodeName = new ThreadLocal<>();

	public static void setReadonly(Boolean bl) {
		Deque<Boolean> deque = readonly.get();
		if (deque == null) {
			deque = new LinkedList<>();
			readonly.set(deque);
		}
		deque.push(bl);
	}

	static Boolean removeReadonly() {
		Deque<Boolean> deque = readonly.get();
		if (deque == null) {
			throw new IllegalStateException("readonly should be present");
		}
		return deque.pop();
	}

	static boolean isReadonly() {
		Deque<Boolean> deque = readonly.get();
		if (deque == null || deque.size() == 0)
			return false;
		return deque.getLast(); // outside scope win.
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