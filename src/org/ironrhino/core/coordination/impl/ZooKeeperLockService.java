package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.ironrhino.core.coordination.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("lockService")
@Profile(CLUSTER)
public class ZooKeeperLockService implements LockService {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/lock";

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private CuratorFramework curatorFramework;

	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private ConcurrentHashMap<String, InterProcessMutex> locks = new ConcurrentHashMap<String, InterProcessMutex>();

	private StandaloneLockService standaloneLockService = new StandaloneLockService();

	public void setZooKeeperPath(String zooKeeperPath) {
		this.zooKeeperPath = zooKeeperPath;
	}

	@Override
	public boolean tryLock(String name) {
		if (standaloneLockService.tryLock(name)) {
			return tryLock(name, 0, TimeUnit.MILLISECONDS);
		} else {
			return false;
		}
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		if (standaloneLockService.tryLock(name)) {
			InterProcessMutex lock = locks.get(name);
			if (lock == null) {
				locks.putIfAbsent(name, new InterProcessMutex(curatorFramework,
						zooKeeperPath + "/" + name));
				lock = locks.get(name);
			}
			boolean success = false;
			try {
				success = lock.acquire(timeout, unit);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			if (!success)
				standaloneLockService.unlock(name);
			return success;
		} else {
			return false;
		}
	}

	@Override
	public void lock(String name) throws Exception {
		standaloneLockService.lock(name);
		InterProcessMutex lock = locks.get(name);
		if (lock == null) {
			locks.putIfAbsent(name, new InterProcessMutex(curatorFramework,
					zooKeeperPath + "/" + name));
			lock = locks.get(name);
		}
		lock.acquire();
	}

	@Override
	public void unlock(String name) {
		standaloneLockService.unlock(name);
		InterProcessMutex lock = locks.get(name);
		if (lock != null && lock.isAcquiredInThisProcess())
			try {
				lock.release();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
	}

}
