package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.LockFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component("lockService")
@ServiceImplementationConditional(profiles = CLUSTER)
public class ZooKeeperLockService implements LockService {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/lock";

	@Autowired
	private CuratorFramework curatorFramework;

	@Setter
	@Value("${lockService.zooKeeperPath:" + DEFAULT_ZOOKEEPER_PATH + "}")
	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private Map<String, InterProcessMutex> locks = new ConcurrentHashMap<>();

	@Override
	public boolean tryLock(String name) {
		return tryLock(name, 0, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		InterProcessMutex lock = locks.computeIfAbsent(name,
				key -> new InterProcessMutex(curatorFramework, zooKeeperPath + '/' + key));
		try {
			return lock.acquire(timeout, unit);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void lock(String name) {
		InterProcessMutex lock = locks.computeIfAbsent(name,
				key -> new InterProcessMutex(curatorFramework, zooKeeperPath + '/' + key));
		try {
			lock.acquire();
		} catch (Exception e) {
			throw new LockFailedException(e);
		}
	}

	@Override
	public void unlock(String name) {
		InterProcessMutex lock = locks.get(name);
		if (lock == null)
			throw new IllegalArgumentException("Lock " + name + " does not exist");
		if (!lock.isAcquiredInThisProcess())
			throw new IllegalStateException("Lock " + name + " is not held");
		try {
			lock.release();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void clearExpired() {
		for (Map.Entry<String, InterProcessMutex> entry : locks.entrySet()) {
			if (!entry.getValue().isAcquiredInThisProcess())
				locks.remove(entry.getKey());
		}
	}

}
