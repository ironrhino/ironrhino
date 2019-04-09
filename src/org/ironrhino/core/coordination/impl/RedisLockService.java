package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
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
	@Value("${lockService.maxHoldTime:18000}")
	private int maxHoldTime = 18000;

	@Getter
	@Value("${lockService.suspiciousHoldTime:600}")
	private int suspiciousHoldTime = 600;

	@Autowired
	@Qualifier("stringRedisTemplate")
	@PriorityQualifier
	private StringRedisTemplate coordinationStringRedisTemplate;

	private RedisScript<Long> compareAndDeleteScript = new DefaultRedisScript<>(
			"if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return redis.call('exists',KEYS[1]) == 0 and 2 or 0 end",
			Long.class);

	@Override
	public boolean tryLock(String name) {
		String key = NAMESPACE + name;
		String holder = holder();
		Boolean success = coordinationStringRedisTemplate.opsForValue().setIfAbsent(key, holder, this.maxHoldTime,
				TimeUnit.SECONDS);
		if (success == null)
			throw new RuntimeException("Unexpected null");
		if (success) {
			return true;
		} else {
			if (AppInfo.getContextPath() == null) // not in servlet container
				return false;
			boolean detectAlive = false;
			if (this.maxHoldTime > this.suspiciousHoldTime) {
				Long value = coordinationStringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
				detectAlive = value != null && this.maxHoldTime - value > this.suspiciousHoldTime;
			}
			if (detectAlive) {
				String currentHolder = coordinationStringRedisTemplate.opsForValue().get(key);
				if (currentHolder == null || currentHolder.startsWith(AppInfo.getInstanceId())) // self
					return false;
				if (!isAlive(currentHolder)) {
					Long ret = coordinationStringRedisTemplate.execute(compareAndDeleteScript,
							Collections.singletonList(key), currentHolder);
					if (ret != null && ret != 0)
						return tryLock(name);
				}
			}
			return false;
		}
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
		Long ret = coordinationStringRedisTemplate.execute(compareAndDeleteScript, Collections.singletonList(key),
				holder);
		if (ret == null)
			throw new RuntimeException("Unexpected null");
		if (ret == 0) {
			throw new IllegalStateException("Lock[" + name + "] is not held by :" + holder);
		} else if (ret == 2) {
			// lock hold timeout
		}
	}

	private static String holder() {
		return AppInfo.getInstanceId() + '$' + Thread.currentThread().getId();
	}

	private static boolean isAlive(String holder) {
		String instanceId = holder.substring(0, holder.lastIndexOf('$'));
		String url = new StringBuilder("http://").append(instanceId.substring(instanceId.lastIndexOf('@') + 1))
				.append("/_ping?_internal_testing_").toString();
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setConnectTimeout(3000);
			conn.setReadTimeout(2000);
			conn.setInstanceFollowRedirects(false);
			conn.setDoOutput(false);
			conn.setUseCaches(false);
			conn.connect();
			if (conn.getResponseCode() == 200) {
				try (BufferedReader br = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					String value = br.lines().collect(Collectors.joining("\n"));
					if (value.equals(instanceId)) {
						return true;
					}
				}
			}
			conn.disconnect();
		} catch (IOException e) {
		}
		return false;
	}

}
