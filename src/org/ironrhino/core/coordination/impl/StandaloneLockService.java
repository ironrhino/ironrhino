package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.DEFAULT;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;

@Component("lockService")
@ServiceImplementationConditional(profiles = DEFAULT)
public class StandaloneLockService implements LockService {

	@Value("${lockService.lockStripes:4}")
	private int lockStripes = 4;

	private Striped<Lock> stripedLocks;

	@PostConstruct
	public void init() {
		stripedLocks = Striped.lazyWeakLock(lockStripes);
	}

	@Override
	public boolean tryLock(String name) {
		return getLock(name).tryLock();
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		Lock lock = getLock(name);
		try {
			return lock.tryLock(timeout, unit);
		} catch (InterruptedException e) {
			return false;
		}
	}

	@Override
	public void lock(String name) {
		getLock(name).lock();
	}

	@Override
	public void unlock(String name) {
		getLock(name).unlock();
	}

	private Lock getLock(String name) {
		return stripedLocks.get(name);
	}
}
