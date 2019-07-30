package org.ironrhino.core.util;

@FunctionalInterface
public interface CheckedCallable<T, E extends Throwable> {

	T call() throws E;

}