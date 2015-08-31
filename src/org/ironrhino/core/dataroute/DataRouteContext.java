package org.ironrhino.core.dataroute;

import java.util.Deque;
import java.util.LinkedList;

public class DataRouteContext {

	public static final int DEFAULT_DATASOURCE_WEIGHT = 1;

	private static ThreadLocal<Deque<Boolean>> readonly = new ThreadLocal<>();

	private static ThreadLocal<Deque<String>> name = new ThreadLocal<>();

	public static void reset() {
		Deque<Boolean> readonlyDeque = readonly.get();
		if (readonlyDeque != null && readonlyDeque.size() > 0)
			readonlyDeque.clear();
		Deque<String> nameDeque = name.get();
		if (nameDeque != null && nameDeque.size() > 0)
			nameDeque.clear();
	}

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

	public static void setName(String s) {
		Deque<String> deque = name.get();
		if (deque == null) {
			deque = new LinkedList<>();
			name.set(deque);
		}
		deque.push(s);
	}

	static String getName() {
		Deque<String> deque = name.get();
		if (deque == null || deque.size() == 0)
			return null;
		return deque.pop();
	}

}