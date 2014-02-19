package org.ironrhino.core.throttle;

import java.util.concurrent.TimeUnit;

public interface ConcurrencyService {

	public boolean tryAcquire(String name, int permits);

	public boolean tryAcquire(String name, int permits, long timeout,
			TimeUnit unit) throws InterruptedException;

	public void acquire(String name, int permits) throws InterruptedException;

	public void release(String name);

}