package org.ironrhino.core.throttle;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;

@RunWith(MockitoJUnitRunner.class)
public class CircuitBreakingTest {

	@Mock
	EchoService echoService;

	@Test(expected = CircuitBreakerOpenException.class)
	public void test() throws Exception {
		AtomicInteger count = new AtomicInteger();
		when(echoService.echo(anyString())).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws IOException {
				if (count.incrementAndGet() > 5)
					throw new IOException("test");
				Object[] args = invocation.getArguments();
				return (String) args[0];
			}
		});
		AtomicInteger success = new AtomicInteger();
		AtomicInteger error = new AtomicInteger();
		for (int i = 0; i < 100; i++) {
			try {
				CircuitBreaking.executeThrowableCallable(this.getClass().getName(), ex -> ex instanceof IOException,
						() -> echoService.echo("test"));
				success.incrementAndGet();
			} catch (IOException e) {
				error.incrementAndGet();
			}
		}
		assertEquals(5, success.get());
		assertEquals(95, error.get());
		CircuitBreaking.executeThrowableCallable(this.getClass().getName(), ex -> ex instanceof IOException,
				() -> echoService.echo("test"));
	}

	public static interface EchoService {

		public String echo(String s) throws IOException;

	}

}
