package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.stereotype.Component;

@Component("lockService")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneLockService implements LockService {

	private Map<String, Long> locks = new ConcurrentHashMap<>();

	@Override
	public boolean tryLock(String name) {
		return locks.putIfAbsent(name, Thread.currentThread().getId()) == null;
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		boolean success = tryLock(name);
		long millisTimeout = unit.toMillis(timeout);
		long start = System.currentTimeMillis();
		while (!success) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return false;
			}
			if ((System.currentTimeMillis() - start) >= millisTimeout)
				break;
			success = tryLock(name);
		}
		return success;
	}

	@Override
	public void lock(String name) {
		tryLock(name, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	@Override
	public void unlock(String name) {
		if (!locks.remove(name, Thread.currentThread().getId())) {
			throw new IllegalStateException(
					"Lock[" + name + "] is not held by thread:" + Thread.currentThread().getName());
		}
	}

}