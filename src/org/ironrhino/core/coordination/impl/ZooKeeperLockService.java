package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("lockService")
@ServiceImplementationConditional(profiles = CLUSTER)
public class ZooKeeperLockService implements LockService {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/lock";

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private CuratorFramework curatorFramework;

	@Value("${lockService.zooKeeperPath:" + DEFAULT_ZOOKEEPER_PATH + "}")
	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private ConcurrentHashMap<String, InterProcessMutex> locks = new ConcurrentHashMap<>();

	@Autowired
	public ZooKeeperLockService(CuratorFramework curatorFramework) {
		this.curatorFramework = curatorFramework;
	}

	public void setZooKeeperPath(String zooKeeperPath) {
		this.zooKeeperPath = zooKeeperPath;
	}

	@Override
	public boolean tryLock(String name) {
		return tryLock(name, 0, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		InterProcessMutex lock = locks.get(name);
		if (lock == null) {
			InterProcessMutex newLock = new InterProcessMutex(curatorFramework, zooKeeperPath + "/" + name);
			lock = locks.putIfAbsent(name, newLock);
			if (lock == null)
				lock = newLock;
		}
		boolean success = false;
		try {
			success = lock.acquire(timeout, unit);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return success;
	}

	@Override
	public void lock(String name) throws Exception {
		InterProcessMutex lock = locks.get(name);
		if (lock == null) {
			InterProcessMutex newLock = new InterProcessMutex(curatorFramework, zooKeeperPath + "/" + name);
			lock = locks.putIfAbsent(name, newLock);
			if (lock == null)
				lock = newLock;
		}
		lock.acquire();
	}

	@Override
	public void unlock(String name) {
		InterProcessMutex lock = locks.get(name);
		if (lock != null && lock.isAcquiredInThisProcess())
			try {
				lock.release();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
	}

}
