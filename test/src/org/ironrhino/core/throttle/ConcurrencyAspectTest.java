package org.ironrhino.core.throttle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ironrhino.core.throttle.ConcurrencyAspectTest.ConcurrencyConfiguration;
import org.ironrhino.core.throttle.impl.StandaloneConcurrencyService;
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
public class ConcurrencyAspectTest {

	@Autowired
	private EchoService echoService;

	@Test(expected = IllegalConcurrentAccessException.class)
	public void test() throws Throwable {
		int concurrency = 10;
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

	public static class EchoService {

		@Concurrency(permits = "5")
		public String echo(String s) throws Exception {
			Thread.sleep(100);
			return s;
		}

	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class ConcurrencyConfiguration {

		@Bean
		public ConcurrencyService concurrencyService() {
			return new StandaloneConcurrencyService();
		}

		@Bean
		public EchoService echoService() {
			return new EchoService();
		}

		@Bean
		public ConcurrencyAspect concurrencyAspect() {
			return new ConcurrencyAspect();
		}

	}
}
