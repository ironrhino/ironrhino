package org.ironrhino.core.util;

@FunctionalInterface
public interface Predicate<T> {
	
	boolean evaluate(T obj);
	
}
