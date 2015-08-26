package org.ironrhino.core.util;

@FunctionalInterface
public interface ObjectFilter {
	public boolean accept(Object object);
}
