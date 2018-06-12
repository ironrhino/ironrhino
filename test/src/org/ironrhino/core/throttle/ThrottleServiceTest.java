package org.ironrhino.core.throttle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.cache.CacheManager;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.ironrhino.core.throttle.ThrottleServiceTest.ConcurrencyConfiguration;
import org.ironrhino.core.throttle.impl.DefaultThrottleService;
import org.ironrhino.core.util.IllegalConcurrentAccessException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConcurrencyConfiguration.class)
public class ThrottleServiceTest {

	@Autowired
	private ThrottleService throttleService;

	@Test(expected = IllegalConcurrentAccessException.class)
	public void test() throws Throwable {
		int concurrency = 2;
		ExecutorService es = Executors.newFixedThreadPool(concurrency);
		Collection<Callable<String>> tasks = new ArrayList<>();
		for (int i = 0; i < concurrency; i++)
			tasks.add(() -> {
				throttleService.delay("test", 2, TimeUnit.SECONDS, 1);
				return null;
			});
		List<Future<String>> results = es.invokeAll(tasks);
		try {
			for (Future<String> f : results) {
				f.get();
			}
		} catch (ExecutionException e) {
			throw e.getCause();
		} finally {
			es.shutdown();
		}
	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class ConcurrencyConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new Cache2kCacheManager();
		}

		@Bean
		public ThrottleService throttleService() {
			return new DefaultThrottleService();
		}

	}
}
