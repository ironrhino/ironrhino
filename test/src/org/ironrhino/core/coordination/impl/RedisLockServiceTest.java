package org.ironrhino.core.coordination.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.coordination.impl.RedisLockServiceTest.RedisLockServiceConfig;
import org.ironrhino.core.util.AppInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisLockServiceConfig.class)
public class RedisLockServiceTest {

	protected static ValueOperations<String, String> opsForValue;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private RedisLockService lockService;

	protected static String holder() {
		return AppInfo.getInstanceId() + '$' + Thread.currentThread().getId();
	}

	@Before
	public void setUp() {
		AppInfo.setContextPath("");
		AppInfo.setHttpPort(8080);
	}

	@Test
	public void testTryLockSuccessful() {
		given(opsForValue.setIfAbsent("lock:key", holder(), lockService.getMaxHoldTime(), TimeUnit.SECONDS))
				.willReturn(true);
		assertThat(lockService.tryLock("key"), is(true));
	}

	@Test
	public void testTryLockFailed() {
		AppInfo.setContextPath(null);
		given(opsForValue.setIfAbsent("lock:key", holder(), lockService.getMaxHoldTime(), TimeUnit.SECONDS))
				.willReturn(false);
		assertThat(lockService.tryLock("key"), is(false));
	}

	@Test
	public void testTryLockWithinSuspiciousHoldTime() {
		given(opsForValue.setIfAbsent("lock:key", holder(), lockService.getMaxHoldTime(), TimeUnit.SECONDS))
				.willReturn(false);
		given(stringRedisTemplate.getExpire("lock:key", TimeUnit.SECONDS))
				.willReturn(lockService.getMaxHoldTime() - lockService.getSuspiciousHoldTime() + 10L);
		assertThat(lockService.tryLock("key"), is(false));
	}

	@Test
	public void testTryLockOverSuspiciousHoldTime() {
		given(opsForValue.setIfAbsent("lock:key", holder(), lockService.getMaxHoldTime(), TimeUnit.SECONDS))
				.willReturn(false);
		given(stringRedisTemplate.getExpire("lock:key", TimeUnit.SECONDS))
				.willReturn(lockService.getMaxHoldTime() - lockService.getSuspiciousHoldTime() - 10L);
		given(opsForValue.get("lock:key")).willReturn("ironrhino-kjdfisf@0.0.0.0:8080$1");
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
				argThat(keys -> keys != null && keys.contains("lock:key")),
				eq("ironrhino-kjdfisf@0.0.0.0:8080$1"))).willAnswer(invocation -> {
					given(opsForValue.setIfAbsent("lock:key", holder(), lockService.getMaxHoldTime(), TimeUnit.SECONDS))
							.willReturn(true);
					return 1L;
				});
		assertThat(lockService.tryLock("key"), is(true));
	}

	@Test
	public void testUnlock() {
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
				argThat(keys -> keys != null && keys.contains("lock:key")), eq(holder())))
						.willReturn(1L);
		lockService.unlock("key");
	}

	static class RedisLockServiceConfig {

		@Bean
		@SuppressWarnings("unchecked")
		public StringRedisTemplate stringRedisTemplate() {
			StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
			given(stringRedisTemplate.opsForValue()).willReturn(opsForValue = mock(ValueOperations.class));
			return stringRedisTemplate;
		}

		@Bean
		public LockService lockService() {
			return new RedisLockService();
		}
	}
}
