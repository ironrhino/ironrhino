package org.ironrhino.core.throttle;

import java.util.concurrent.TimeUnit;

public interface ConcurrencyService {

	boolean tryAcquire(String name, int permits);

	boolean tryAcquire(String name, int permits, long timeout, TimeUnit unit) throws InterruptedException;

	void acquire(String name, int permits) throws InterruptedException;

	void release(String name);

}