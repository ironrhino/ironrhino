package org.ironrhino.core.util;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface CheckedCallable<T, E extends Throwable> {

	T call() throws E;

	default Callable<T> uncheck() {
		return () -> {
			try {
				return call();
			} catch (Throwable e) {
				return ExceptionUtils.sneakyThrow(e);
			}
		};
	}

	static <T, E extends Throwable> Callable<T> unchecked(CheckedCallable<T, E> callable) {
		return requireNonNull(callable).uncheck();
	}

}