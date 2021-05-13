package org.ironrhino.core.coordination;

import java.util.concurrent.TimeUnit;

public interface LockService {

	boolean tryLock(String name);

	default boolean tryLock(String name, long timeout, TimeUnit unit) {
		long millisTimeout = unit.toMillis(timeout);
		long start = System.nanoTime();
		while (true) {
			if (tryLock(name))
				return true;
			if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= millisTimeout)
				return false;
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return false;
			}
		}
	}

	default void lock(String name) {
		tryLock(name, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	void unlock(String name);

}