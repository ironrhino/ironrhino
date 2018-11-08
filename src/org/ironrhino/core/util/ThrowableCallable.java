package org.ironrhino.core.util;

@FunctionalInterface
public interface ThrowableCallable<T, E extends Throwable> {

	T call() throws E;

}