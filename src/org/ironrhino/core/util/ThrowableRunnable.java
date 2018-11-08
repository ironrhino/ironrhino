package org.ironrhino.core.util;

@FunctionalInterface
public interface ThrowableRunnable<E extends Throwable> {

	void run() throws E;

	static <E extends Throwable> Runnable unchecked(ThrowableRunnable<E> runnable) {
		return () -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

}