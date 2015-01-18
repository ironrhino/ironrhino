package org.ironrhino.core.throttle.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.throttle.ConcurrencyService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("concurrencyService")
@Profile(DEFAULT)
@ResourcePresentConditional(value = "resources/spring/applicationContext-throttle.xml", negated = true)
public class StandaloneConcurrencyService implements ConcurrencyService {

	private ConcurrentHashMap<String, Semaphore> semaphores = new ConcurrentHashMap<String, Semaphore>();

	@Override
	public boolean tryAcquire(String name, int permits) {
		return getSemaphore(name, permits).tryAcquire();
	}

	@Override
	public boolean tryAcquire(String name, int permits, long timeout,
			TimeUnit unit) throws InterruptedException {
		return getSemaphore(name, permits).tryAcquire(timeout, unit);
	}

	@Override
	public void acquire(String name, int permits) throws InterruptedException {
		getSemaphore(name, permits).acquire();
	}

	@Override
	public void release(String name) {
		Semaphore semaphore = semaphores.get(name);
		if (semaphore == null)
			throw new IllegalArgumentException("Semaphore '" + name
					+ " ' doesn't exists");
		semaphore.release();
	}

	private Semaphore getSemaphore(String name, int permits) {
		Semaphore semaphore = semaphores.get(name);
		if (semaphore == null) {
			Semaphore newSemaphore = new Semaphore(permits);
			semaphore = semaphores.putIfAbsent(name, newSemaphore);
			if (semaphore == null)
				semaphore = newSemaphore;
		}
		return semaphore;
	}
}
