package org.ironrhino.core.aop;

import java.util.ArrayList;
import java.util.List;

public class AopContext {

	private static ThreadLocal<List<Class<?>>> bypass = new ThreadLocal<List<Class<?>>>();

	public static void reset() {
		bypass.remove();
	}

	public static void setBypass(Class<?> clazz) {
		List<Class<?>> list = bypass.get();
		if (list == null)
			list = new ArrayList<Class<?>>(5);
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
