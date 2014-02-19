package org.ironrhino.core.coordination;

import java.util.concurrent.TimeUnit;

public interface LockService {

	public boolean tryLock(String name);

	public boolean tryLock(String name, long timeout, TimeUnit unit);

	public void lock(String name) throws Exception;

	public void unlock(String name);

}