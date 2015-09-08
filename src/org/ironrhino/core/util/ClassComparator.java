package org.ironrhino.core.util;

import java.util.Comparator;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

public class ClassComparator implements Comparator<Class<?>> {

	public final static ClassComparator INSTANCE = new ClassComparator();

	@Override
	public int compare(Class<?> a, Class<?> b) {
		int order1 = Ordered.LOWEST_PRECEDENCE, order2 = Ordered.LOWEST_PRECEDENCE;
		Order o = a.getAnnotation(Order.class);
		if (o != null)
			order1 = o.value();
		o = b.getAnnotation(Order.class);
		if (o != null)
			order2 = o.value();
		int v = order1 == order2 ? 0 : order1 < order2 ? -1 : 1;
		return v != 0 ? v : b.getName().split("\\.").length - a.getName().split("\\.").length;
	}

}
