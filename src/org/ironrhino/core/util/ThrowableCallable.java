package org.ironrhino.core.util;

public interface ThrowableCallable<T> {

	T call() throws Throwable;

}