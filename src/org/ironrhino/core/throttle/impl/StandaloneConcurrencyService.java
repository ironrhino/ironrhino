package org.ironrhino.core.throttle.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.throttle.ConcurrencyService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("concurrencyService")
@Profile(DEFAULT)
public class StandaloneConcurrencyService implements ConcurrencyService {

	private ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<String, Semaphore>();

	@Override
	public boolean tryAcquire(String name, int permits) {
		Semaphore semaphore = semaphores.get(name);
		if (semaphore == null) {
			semaphores.putIfAbsent(name, new Semaphore(permits));
			semaphore = semaphores.get(name);
		}
		return semaphore.tryAcquire();
	}

	@Override
	public boolean tryAcquire(String name, int permits, long timeout,
			TimeUnit unit) throws InterruptedException {
		Semaphore semaphore = semaphores.get(name);
		if (semaphore == null) {
			semaphores.putIfAbsent(name, new Semaphore(permits));
			semaphore = semaphores.get(name);
		}
		return semaphore.tryAcquire(timeout, unit);
	}

	@Override
	public void acquire(String name, int permits) throws InterruptedException {
		Semaphore semaphore = semaphores.get(name);
		if (semaphore == null) {
			semaphores.putIfAbsent(name, new Semaphore(permits));
			semaphore = semaphores.get(name);
		}
		semaphore.acquire();
	}

	@Override
	public void release(String name) {
		Semaphore semaphore = semaphores.get(name);
		if (semaphore == null)
			throw new IllegalArgumentException("Semaphore '" + name
					+ " ' doesn't exists");
		semaphore.release();
	}
}
