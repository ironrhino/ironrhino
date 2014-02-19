package org.ironrhino.core.dataroute;

import java.util.Deque;
import java.util.LinkedList;

public class DataRouteContext {

	public static final int DEFAULT_DATASOURCE_WEIGHT = 1;

	private static ThreadLocal<Deque<Boolean>> readonly = new ThreadLocal<Deque<Boolean>>();

	private static ThreadLocal<Deque<String>> name = new ThreadLocal<Deque<String>>();

	public static void reset() {
		readonly.remove();
		name.remove();
	}

	public static void setReadonly(boolean bl) {
		Deque<Boolean> deque = readonly.get();
		if (deque == null) {
			deque = new LinkedList<Boolean>();
			readonly.set(deque);
		}
		deque.push(bl);
	}

	public static boolean isReadonly() {
		Deque<Boolean> deque = readonly.get();
		if (deque == null || deque.size() == 0)
			return false;
		return deque.pop();
	}

	public static void setName(String s) {
		Deque<String> deque = name.get();
		if (deque == null) {
			deque = new LinkedList<String>();
			name.set(deque);
		}
		deque.push(s);
	}

	public static String getName() {
		Deque<String> deque = name.get();
		if (deque == null || deque.size() == 0)
			return null;
		return deque.pop();
	}

}