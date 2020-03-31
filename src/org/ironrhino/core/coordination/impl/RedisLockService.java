package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.NameableThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component("lockService")
@ServiceImplementationConditional(profiles = { DUAL, CLOUD })
public class RedisLockService implements LockService {

	private static final String NAMESPACE = "lock:";

	@Getter
	@Value("${lockService.watchdogTimeout:30000}")
	private int watchdogTimeout = 30000;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate coordinationStringRedisTemplate;

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1,
			new NameableThreadFactory("redis-lock"));

	private Map<String, ScheduledFuture<?>> renewalFutures = new ConcurrentHashMap<>();

	private RedisScript<Long> compareAndDeleteScript = new DefaultRedisScript<>(
			"if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return redis.call('exists',KEYS[1]) == 0 and 2 or 0 end",
			Long.class);

	@PreDestroy
	private void destroy() {
		scheduler.shutdown();
	}

	@Override
	public boolean tryLock(String name) {
		String key = NAMESPACE + name;
		String holder = holder();
		Boolean success = coordinationStringRedisTemplate.opsForValue().setIfAbsent(key, holder, this.watchdogTimeout,
				TimeUnit.MILLISECONDS);
		if (success == null)
			throw new RuntimeException("Unexpected null");
		if (success) {
			long delay = watchdogTimeout / 3;
			renewalFutures.computeIfAbsent(name, k -> scheduler.scheduleWithFixedDelay(() -> {
				Boolean b = coordinationStringRedisTemplate.expire(key, this.watchdogTimeout, TimeUnit.MILLISECONDS);
				if (b == null)
					throw new RuntimeException("Unexpected null");
				if (!b) {
					ScheduledFuture<?> future = renewalFutures.remove(name);
					if (future != null)
						future.cancel(true);
				}
			}, delay, delay, TimeUnit.MILLISECONDS));
			return true;
		}
		return false;
	}

	@Override
	public void unlock(String name) {
		String key = NAMESPACE + name;
		String holder = holder();
		Long ret = coordinationStringRedisTemplate.execute(compareAndDeleteScript, Collections.singletonList(key),
				holder);
		if (ret == null)
			throw new RuntimeException("Unexpected null");
		if (ret == 1) {
			ScheduledFuture<?> future = renewalFutures.remove(name);
			if (future != null)
				future.cancel(true);
		} else if (ret == 0) {
			throw new IllegalStateException("Lock[" + name + "] is not held by :" + holder);
		} else if (ret == 2) {
			// lock hold timeout
		}
	}

	static String holder() {
		return AppInfo.getInstanceId() + '$' + Thread.currentThread().getId();
	}

}
