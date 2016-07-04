package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component("lockService")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
public class RedisLockService implements LockService {

	private static final String NAMESPACE = "lock:";

	@Autowired
	private Logger logger;

	@Value("${lockService.maxHoldTime:300}")
	private int maxHoldTime = 300;

	@Autowired(required = false)
	@Qualifier("coordinationStringRedisTemplate")
	private RedisTemplate<String, String> coordinationStringRedisTemplate;

	@Autowired
	@Qualifier("stringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@PostConstruct
	public void afterPropertiesSet() {
		if (coordinationStringRedisTemplate != null)
			stringRedisTemplate = coordinationStringRedisTemplate;
	}

	@Override
	public boolean tryLock(String name) {
		String key = NAMESPACE + name;
		String value = AppInfo.getInstanceId();
		boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
		if (success)
			stringRedisTemplate.expire(key, this.maxHoldTime, TimeUnit.SECONDS);
		return success;
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		if (timeout <= 0)
			return tryLock(name);
		String key = NAMESPACE + name;
		String value = AppInfo.getInstanceId();
		boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
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
			success = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
		}
		if (success)
			stringRedisTemplate.expire(key, this.maxHoldTime, TimeUnit.SECONDS);
		return success;
	}

	@Override
	public void lock(String name) {
		String key = NAMESPACE + name;
		String value = AppInfo.getInstanceId();
		boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
		while (!success) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			success = stringRedisTemplate.opsForValue().setIfAbsent(key, value);
			if (success)
				stringRedisTemplate.expire(key, this.maxHoldTime, TimeUnit.SECONDS);
		}

	}

	@Override
	public void unlock(String name) {
		String key = NAMESPACE + name;
		String value = AppInfo.getInstanceId();
		String str = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
		RedisScript<Long> script = new DefaultRedisScript<>(str, Long.class);
		Long ret = stringRedisTemplate.execute(script, Collections.singletonList(key), value);
		if (ret == 0)
			logger.error("Lock [{}] is not hold by instance [{}]", name, value);
	}

}
