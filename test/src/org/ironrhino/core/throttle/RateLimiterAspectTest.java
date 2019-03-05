package org.ironrhino.core.throttle;

import org.ironrhino.core.throttle.RateLimiterAspectTest.RateLimiterConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RateLimiterConfiguration.class)
public class RateLimiterAspectTest {

	@Autowired
	private TestService testService;

	@Test
	public void testPermit() throws Exception {
		Thread.sleep(500);
		for (int i = 0; i < 100; i++) {
			testService.test();
		}
	}

	@Test(expected = RequestNotPermitted.class)
	public void testNotPermit() throws Exception {
		Thread.sleep(500);
		for (int i = 0; i < 101; i++) {
			testService.test();
		}
	}

	public static class TestService {
		@RateLimiter(timeoutDuration = 0, limitForPeriod = 100)
		public void test() {
		}

	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class RateLimiterConfiguration {

		@Bean
		public TestService testService() {
			return new TestService();
		}

		@Bean
		public RateLimiterAspect rateLimiterAspect() {
			return new RateLimiterAspect();
		}

	}
}
