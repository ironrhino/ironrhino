package org.ironrhino.core.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Throwable> {

	void accept(T t) throws E;

	static <T, E extends Throwable> Consumer<T> unchecked(CheckedConsumer<T, E> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

}