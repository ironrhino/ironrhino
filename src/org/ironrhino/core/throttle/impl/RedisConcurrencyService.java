package org.ironrhino.core.throttle.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.throttle.ConcurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("concurrencyService")
@Profile({ DUAL, CLOUD, CLUSTER })
public class RedisConcurrencyService implements ConcurrencyService {

	private static final String NAMESPACE = "concurrency:";

	protected Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Override
	public boolean tryAcquire(String name, int permits) {
		String key = NAMESPACE + name;
		boolean success = stringRedisTemplate.opsForValue().increment(key, 1)
				.intValue() <= permits;
		if (!success)
			stringRedisTemplate.opsForValue().increment(key, -1);
		return success;
	}

	@Override
	public boolean tryAcquire(String name, int permits, long timeout,
			TimeUnit unit) throws InterruptedException {
		if (timeout <= 0)
			return tryAcquire(name, permits);
		String key = NAMESPACE + name;
		boolean success = stringRedisTemplate.opsForValue().increment(key, 1)
				.intValue() <= permits;
		if (!success)
			stringRedisTemplate.opsForValue().increment(key, -1);
		long millisTimeout = unit.toMillis(timeout);
		long start = System.currentTimeMillis();
		while (!success) {
			Thread.sleep(100);
			if ((System.currentTimeMillis() - start) >= millisTimeout)
				break;
			success = stringRedisTemplate.opsForValue().increment(key, 1)
					.intValue() <= permits;
			if (!success)
				stringRedisTemplate.opsForValue().increment(key, -1);
		}
		return success;
	}

	@Override
	public void acquire(String name, int permits) {
		String key = NAMESPACE + name;
		boolean success = stringRedisTemplate.opsForValue().increment(key, 1)
				.intValue() <= permits;
		if (!success)
			stringRedisTemplate.opsForValue().increment(key, -1);
		while (!success) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			success = stringRedisTemplate.opsForValue().increment(key, 1)
					.intValue() <= permits;
			if (!success)
				stringRedisTemplate.opsForValue().increment(key, -1);
		}

	}

	@Override
	public void release(String name) {
		String key = NAMESPACE + name;
		stringRedisTemplate.opsForValue().increment(key, -1);
	}

}
