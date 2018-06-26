package org.ironrhino.core.util;

public interface ThrowableCallable<T, E extends Throwable> {

	T call() throws E;

}