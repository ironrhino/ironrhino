package org.ironrhino.core.coordination;

import java.util.concurrent.TimeUnit;

public interface LockService {

	boolean tryLock(String name);

	default boolean tryLock(String name, long timeout, TimeUnit unit) {
		boolean success = tryLock(name);
		long millisTimeout = unit.toMillis(timeout);
		long start = System.nanoTime();
		while (!success) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return false;
			}
			if (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) >= millisTimeout)
				break;
			success = tryLock(name);
		}
		return success;
	}

	default void lock(String name) {
		tryLock(name, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	void unlock(String name);

}