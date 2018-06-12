package org.ironrhino.core.throttle;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.throttle.CircuitBreakerAspectTest.CircuitBreakerConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CircuitBreakerConfiguration.class)
public class CircuitBreakerAspectTest {

	@Autowired
	private EchoService echoService;

	@Test(expected = CircuitBreakerOpenException.class)
	public void test() throws Exception {
		for (int i = 0; i < 10000; i++) {
			try {
				echoService.echo("test");
			} catch (IOException e) {

			}
		}
	}

	public static class EchoService {

		public AtomicInteger count = new AtomicInteger();

		@CircuitBreaker(include = IOException.class)
		public String echo(String s) throws IOException {
			if (count.getAndIncrement() % 2 == 1)
				throw new IOException("for test");
			return s;
		}

	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class CircuitBreakerConfiguration {

		@Bean
		public EchoService echoService() {
			return new EchoService();
		}

		@Bean
		public CircuitBreakerAspect circuitBreakerAspect() {
			return new CircuitBreakerAspect();
		}

	}
}
