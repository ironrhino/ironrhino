package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.coordination.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("lockService")
@Profile({ DUAL, CLOUD })
public class RedisLockService implements LockService {

	private static final String NAMESPACE = "lock:";

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Value("${lockService.timeout:300}")
	private int timeout = 300;

	private StandaloneLockService standaloneLockService = new StandaloneLockService();

	@Override
	public boolean tryLock(String name) {
		if (standaloneLockService.tryLock(name)) {
			String key = NAMESPACE + name;
			boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
					key, "");
			if (success)
				stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
			else
				standaloneLockService.unlock(name);
			return success;
		} else {
			return false;
		}
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		if (timeout <= 0)
			return tryLock(name);
		if (standaloneLockService.tryLock(name, timeout, unit)) {
			String key = NAMESPACE + name;
			boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
					key, "");
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
				success = stringRedisTemplate.opsForValue()
						.setIfAbsent(key, "");
			}
			if (success)
				stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
			else
				standaloneLockService.unlock(name);
			return success;
		} else {
			return false;
		}
	}

	@Override
	public void lock(String name) {
		standaloneLockService.lock(name);
		String key = NAMESPACE + name;
		boolean success = stringRedisTemplate.opsForValue()
				.setIfAbsent(key, "");
		while (!success) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			success = stringRedisTemplate.opsForValue().setIfAbsent(key, "");
			if (success)
				stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
		}

	}

	@Override
	public void unlock(String name) {
		standaloneLockService.unlock(name);
		String key = NAMESPACE + name;
		stringRedisTemplate.delete(key);
	}

}
