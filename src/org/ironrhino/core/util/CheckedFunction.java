package org.ironrhino.core.util;

import static java.util.Objects.requireNonNull;

import java.util.function.Function;

@FunctionalInterface
public interface CheckedFunction<T, R, E extends Throwable> {

	R apply(T t) throws E;

	default Function<T, R> uncheck() {
		return t -> {
			try {
				return apply(t);
			} catch (Throwable e) {
				return ExceptionUtils.sneakyThrow(e);
			}
		};
	}

	static <T, R, E extends Throwable> Function<T, R> unchecked(CheckedFunction<T, R, E> function) {
		return requireNonNull(function).uncheck();
	}

}