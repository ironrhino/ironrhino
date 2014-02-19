package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ironrhino.core.coordination.LockService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("lockService")
@Profile(DEFAULT)
public class StandaloneLockService implements LockService {

	private ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();

	@Override
	public boolean tryLock(String name) {
		Lock lock = locks.get(name);
		if (lock == null) {
			locks.putIfAbsent(name, new ReentrantLock());
			lock = locks.get(name);
		}
		return lock.tryLock();
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		Lock lock = locks.get(name);
		if (lock == null) {
			locks.putIfAbsent(name, new ReentrantLock());
			lock = locks.get(name);
		}
		try {
			return lock.tryLock(timeout, unit);
		} catch (InterruptedException e) {
			return false;
		}
	}

	@Override
	public void lock(String name) {
		Lock lock = locks.get(name);
		if (lock == null) {
			locks.putIfAbsent(name, new ReentrantLock());
			lock = locks.get(name);
		}
		lock.lock();
	}

	@Override
	public void unlock(String name) {
		Lock lock = locks.get(name);
		if (lock != null)
			lock.unlock();
	}
}
