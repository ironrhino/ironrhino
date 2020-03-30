package org.ironrhino.core.coordination.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.concurrent.TimeUnit;

import org.ironrhino.core.coordination.LockService;
import org.ironrhino.core.coordination.impl.RedisLockServiceTest.RedisLockServiceConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	@SuppressWarnings("unchecked")
	@Before
	public void cleanup() {
		reset(stringRedisTemplate);
		given(stringRedisTemplate.opsForValue()).willReturn(opsForValue = mock(ValueOperations.class));
	}

	@Test
	public void testTryLockSuccessful() {
		given(opsForValue.setIfAbsent("lock:key", RedisLockService.holder(), lockService.getWatchdogTimeout(),
				TimeUnit.MILLISECONDS)).willReturn(true);
		assertThat(lockService.tryLock("key"), is(true));
	}

	@Test
	public void testTryLockFailed() {
		given(opsForValue.setIfAbsent("lock:key", RedisLockService.holder(), lockService.getWatchdogTimeout(),
				TimeUnit.MILLISECONDS)).willReturn(false);
		assertThat(lockService.tryLock("key"), is(false));
	}

	@Test
	public void testUnlockSuccessful() {
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
				argThat(keys -> keys != null && keys.contains("lock:key")), eq(RedisLockService.holder())))
						.willReturn(1L);
		lockService.unlock("key");
	}

	@Test(expected = IllegalStateException.class)
	public void testUnlockFailed() {
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
				argThat(keys -> keys != null && keys.contains("lock:key")), eq(RedisLockService.holder())))
						.willReturn(0L);
		lockService.unlock("key");
	}

	@Configuration
	static class RedisLockServiceConfig {

		@Bean
		public StringRedisTemplate stringRedisTemplate() {
			return mock(StringRedisTemplate.class);
		}

		@Bean
		public LockService lockService() {
			return new RedisLockService();
		}
	}
}
