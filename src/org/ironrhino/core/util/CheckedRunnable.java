package org.ironrhino.core.util;

@FunctionalInterface
public interface CheckedRunnable<E extends Throwable> {

	void run() throws E;

	static <E extends Throwable> Runnable unchecked(CheckedRunnable<E> runnable) {
		return () -> {
			try {
				runnable.run();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

}