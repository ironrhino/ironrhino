package org.ironrhino.core.coordination;

import java.util.concurrent.TimeUnit;

public interface LockService {

	boolean tryLock(String name);

	boolean tryLock(String name, long timeout, TimeUnit unit);

	void lock(String name);

	void unlock(String name);

}