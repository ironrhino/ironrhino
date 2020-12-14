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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FrequencyAspectTest.Config.class)
public class FrequencyAspectTest {

	private static final int LIMITS = 5;

	@Autowired
	private EchoService echoService;

	@Autowired
	private Cache2kCacheManager cacheManager;

	@Before
	public void before() {
		cacheManager.invalidate("frequency");
	}

	@Test
	public void testSequentialUnderLmimit() throws Throwable {
		for (int i = 0; i < LIMITS; i++)
			echoService.echo("test");
	}

	@Test(expected = FrequencyLimitExceededException.class)
	public void testSequentialBeyondLmimit() throws Throwable {
		for (int i = 0; i < 2 * LIMITS; i++)
			echoService.echo("test");
	}

	@Test
	public void testSequentialBeyondLmimitButWithDelay() throws Throwable {
		for (int i = 0; i < 2 * LIMITS; i++) {
			Thread.sleep(400);
			echoService.echo("test");
		}
	}

	@Test(expected = FrequencyLimitExceededException.class)
	public void testConcurrent() throws Throwable {
		int concurrency = LIMITS + 1;
		ExecutorService es = Executors.newFixedThreadPool(concurrency);
		Collection<Callable<String>> tasks = new ArrayList<>();
		for (int i = 0; i < concurrency; i++)
			tasks.add(() -> echoService.echo("test"));
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

	@Test
	public void testRecover() throws Throwable {
		int concurrency = LIMITS + 1;
		ExecutorService es = Executors.newFixedThreadPool(concurrency);
		Collection<Callable<String>> tasks = new ArrayList<>();
		for (int i = 0; i < LIMITS + 1; i++)
			tasks.add(() -> echoService.echo("test"));
		List<Future<String>> results = es.invokeAll(tasks);
		Thread.sleep(2410);
		tasks = new ArrayList<>();
		for (int i = 0; i < concurrency / 2; i++)
			tasks.add(() -> echoService.echo("test"));
		results = es.invokeAll(tasks);
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

	public static class EchoService {

		@Frequency(limits = "" + LIMITS, duration = 2, timeUnit = TimeUnit.SECONDS)
		public String echo(String s) throws Exception {
			Thread.sleep(50);
			return s;
		}

	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class Config {

		@Bean
		public CacheManager cacheManager() {
			return new Cache2kCacheManager();
		}

		@Bean
		public EchoService echoService() {
			return new EchoService();
		}

		@Bean
		public FrequencyAspect frequencyAspect() {
			return new FrequencyAspect();
		}

	}
}
