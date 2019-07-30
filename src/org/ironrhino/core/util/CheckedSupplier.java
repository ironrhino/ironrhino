package org.ironrhino.core.util;

import java.util.function.Supplier;

@FunctionalInterface
public interface CheckedSupplier<T, E extends Throwable> {

	T get() throws E;

	static <T, E extends Throwable> Supplier<T> unchecked(CheckedSupplier<T, E> supplier) {
		return () -> {
			try {
				return supplier.get();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

}