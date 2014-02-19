package org.ironrhino.core.model;

@SuppressWarnings("rawtypes")
public interface Ordered<T extends Ordered> extends Comparable<T> {

	public int getDisplayOrder();
}
