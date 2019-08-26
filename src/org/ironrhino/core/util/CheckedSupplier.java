package org.ironrhino.core.util;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;

@FunctionalInterface
public interface CheckedSupplier<T, E extends Throwable> {

	T get() throws E;

	default Supplier<T> uncheck() {
		return () -> {
			try {
				return get();
			} catch (Throwable e) {
				return ExceptionUtils.sneakyThrow(e);
			}
		};
	}

	static <T, E extends Throwable> Supplier<T> unchecked(CheckedSupplier<T, E> supplier) {
		return requireNonNull(supplier).uncheck();
	}

}