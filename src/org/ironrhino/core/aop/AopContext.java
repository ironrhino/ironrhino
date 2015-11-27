package org.ironrhino.core.aop;

import java.util.ArrayList;
import java.util.List;

public class AopContext {

	public static final String CONTEXT_KEY_THIS = "_this_";

	public static final String CONTEXT_KEY_ARGS = "_args_";

	public static final String CONTEXT_KEY_USER = "_user_";

	private static ThreadLocal<List<Class<?>>> bypass = new ThreadLocal<>();

	public static void reset() {
		bypass.remove();
	}

	public static void setBypass(Class<?> clazz) {
		List<Class<?>> list = bypass.get();
		if (list == null)
			list = new ArrayList<>(5);
		list.add(clazz);
		bypass.set(list);
	}

	public static boolean isBypass(Class<?> clazz) {
		List<Class<?>> list = bypass.get();
		if (list == null) {
			return false;
		}
		boolean bl = list.contains(clazz);
		if (bl)
			list.remove(clazz);
		return bl;
	}

}
