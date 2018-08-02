package org.ironrhino.core.util;

public interface ThrowableFunction<T, R, E extends Throwable> {

	R apply(T t) throws E;

}