package org.ironrhino.core.throttle;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;

public class CircuitBreakingTest {

	@Test(expected = CircuitBreakerOpenException.class)
	public void test() throws Exception {
		EchoService echoService = new EchoService();
		for (int i = 0; i < 10000; i++) {
			try {
				echoService.echo("test");
			} catch (IOException e) {

			}
		}
	}

	public static class EchoService {

		public AtomicInteger count = new AtomicInteger();

		public String echo(String s) throws IOException {
			return CircuitBreaking.executeThrowableCallable(this.getClass().getName(), ex -> ex instanceof IOException,
					() -> doEcho(s));
		}

		private String doEcho(String s) throws IOException {
			if (Math.abs(count.getAndIncrement()) % 2 == 1)
				throw new IOException("for test");
			return s;

		}

	}

}
