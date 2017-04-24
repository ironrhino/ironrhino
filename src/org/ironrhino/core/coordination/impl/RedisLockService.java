package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
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

	@Value("${lockService.maxHoldTime:300}")
	private int maxHoldTime = 300;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier("coordinationStringRedisTemplate")
	private RedisTemplate<String, String> stringRedisTemplate;

	@Override
	public boolean tryLock(String name) {
		String key = NAMESPACE + name;
		String holder = holder();
		boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, holder);
		if (success)
			stringRedisTemplate.expire(key, this.maxHoldTime, TimeUnit.SECONDS);
		return success;
	}

	@Override
	public boolean tryLock(String name, long timeout, TimeUnit unit) {
		boolean success = tryLock(name);
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
			success = tryLock(name);
		}
		return success;
	}

	@Override
	public void lock(String name) {
		tryLock(name, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
	}

	@Override
	public void unlock(String name) {
		String key = NAMESPACE + name;
		String holder = holder();
		String str = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
		RedisScript<Long> script = new DefaultRedisScript<>(str, Long.class);
		Long ret = stringRedisTemplate.execute(script, Collections.singletonList(key), holder);
		if (ret == 0)
			throw new IllegalStateException("Lock[" + name + "] is not held by :" + holder);
	}

	private String holder() {
		return AppInfo.getInstanceId() + '$' + Thread.currentThread().getId();
	}

}
