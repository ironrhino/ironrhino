package org.ironrhino.core.throttle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
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

	@Test
	public void test() throws Exception {
		boolean opened = false;
		AtomicInteger success = new AtomicInteger();
		AtomicInteger fail = new AtomicInteger();
		try {
			for (int i = 0; i < EchoService.SIZE_IN_CLOSED_STATE + 1; i++) {
				try {
					echoService.echo("test");
					success.incrementAndGet();
				} catch (IOException e) {
					fail.incrementAndGet();
				}
			}
		} catch (CircuitBreakerOpenException ex) {
			opened = true;
		}
		assertThat(success.get(), is(EchoService.SIZE_IN_CLOSED_STATE / 2));
		assertThat(fail.get(), is(EchoService.SIZE_IN_CLOSED_STATE / 2));
		assertThat(opened, is(true));
		echoService.recover(true);
		assertThat(isOpen(), is(true));
		Thread.sleep(TimeUnit.SECONDS.toMillis(EchoService.WAIT_DURATION_IN_OPEN_STATE) / 2);
		assertThat(isOpen(), is(true));
		Thread.sleep(TimeUnit.SECONDS.toMillis(EchoService.WAIT_DURATION_IN_OPEN_STATE) / 2);
		for (int i = 0; i < 100; i++) {
			assertThat(echoService.echo("test"), is("test"));
		}
	}

	@Test
	public void testHalfOpen() throws InterruptedException {
		boolean opened = false;
		AtomicInteger success = new AtomicInteger();
		AtomicInteger fail = new AtomicInteger();
		echoService.recover(false);
		try {
			for (int i = 0; i < EchoService.SIZE_IN_CLOSED_STATE + 1; i++) {
				try {
					echoService.echo("test");
					success.incrementAndGet();
				} catch (IOException e) {
					fail.incrementAndGet();
				}
			}
		} catch (CircuitBreakerOpenException ex) {
			opened = true;
		}
		assertThat(success.get(), is(EchoService.SIZE_IN_CLOSED_STATE / 2));
		assertThat(fail.get(), is(EchoService.SIZE_IN_CLOSED_STATE / 2));
		assertThat(opened, is(true));
		Thread.sleep(TimeUnit.SECONDS.toMillis(EchoService.WAIT_DURATION_IN_OPEN_STATE));
		opened = false;
		try {
			for (int i = 0; i < EchoService.SIZE_IN_HALF_OPEN_STATE + 1; i++) {
				try {
					echoService.echo("test");
					success.incrementAndGet();
				} catch (IOException e) {
					fail.incrementAndGet();
				}
			}
		} catch (CircuitBreakerOpenException ex) {
			opened = true;
		}
		assertThat(opened, is(true));
		assertThat(success.get(), is((EchoService.SIZE_IN_CLOSED_STATE + EchoService.SIZE_IN_HALF_OPEN_STATE) / 2));
		assertThat(fail.get(), is((EchoService.SIZE_IN_CLOSED_STATE + EchoService.SIZE_IN_HALF_OPEN_STATE) / 2));
	}

	private boolean isOpen() {
		try {
			echoService.echo("test");
		} catch (CircuitBreakerOpenException ex) {
			return true;
		} catch (IOException ex) {
			return false;
		}
		return false;
	}

	public static class EchoService {

		public static final int SIZE_IN_CLOSED_STATE = 10;
		public static final int SIZE_IN_HALF_OPEN_STATE = 6;
		public static final int WAIT_DURATION_IN_OPEN_STATE = 2;

		public AtomicInteger count = new AtomicInteger();

		private volatile boolean recovered;

		@CircuitBreaker(include = IOException.class, failureRateThreshold = 50, waitDurationInOpenState = WAIT_DURATION_IN_OPEN_STATE, ringBufferSizeInClosedState = SIZE_IN_CLOSED_STATE, ringBufferSizeInHalfOpenState = SIZE_IN_HALF_OPEN_STATE)
		public String echo(String s) throws IOException {
			if (recovered)
				return s;
			if (Math.abs(count.getAndIncrement()) % 2 == 1)
				throw new IOException("for test");
			return s;
		}

		public void recover(boolean recovered) {
			this.recovered = recovered;
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

		@Bean
		public CircuitBreakerRegistry circuitBreakerRegistry() {
			return new CircuitBreakerRegistry();
		}

	}
}
