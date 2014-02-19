package org.ironrhino.core.util;

import java.util.Comparator;
import java.util.Map;

public class ValueThenKeyComparator<K, V> implements
		Comparator<Map.Entry<K, V>> {

	@SuppressWarnings("rawtypes")
	private final static ValueThenKeyComparator DEFAULT_INSTANCE = new ValueThenKeyComparator();

	@SuppressWarnings("unchecked")
	public static <K, V> ValueThenKeyComparator<K, V> getDefaultInstance() {
		return DEFAULT_INSTANCE;
	}

	@Override
	public int compare(Map.Entry<K, V> a, Map.Entry<K, V> b) {
		int v = compareValue(a.getValue(), b.getValue());
		return (v != 0) ? v : compareKey(a.getKey(), b.getKey());
	}

	@SuppressWarnings("unchecked")
	protected int compareValue(V a, V b) {
		return (a instanceof Comparable) ? ((Comparable<V>) a).compareTo(b)
				: String.valueOf(a).compareTo(String.valueOf(b));
	}

	@SuppressWarnings("unchecked")
	protected int compareKey(K a, K b) {
		return (a instanceof Comparable) ? ((Comparable<K>) a).compareTo(b)
				: String.valueOf(a).compareTo(String.valueOf(b));
	}

}
