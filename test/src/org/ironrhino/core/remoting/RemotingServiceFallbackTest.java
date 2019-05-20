package org.ironrhino.core.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.net.ConnectException;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.remoting.RemotingServiceFallbackTest.RemotingFallbackConfiguration;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.client.RemotingServiceRegistryPostProcessor;
import org.ironrhino.core.remoting.impl.StandaloneServiceRegistry;
import org.ironrhino.core.spring.configuration.Fallback;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RemotingFallbackConfiguration.class)
public class RemotingServiceFallbackTest {

	@Autowired
	private TestService testService;

	@Autowired
	private EchoService echoService;

	@Test
	public void testServiceNotFoundException() {
		assertFalse(testService instanceof FallbackTestService);
		assertTrue(Proxy.isProxyClass(testService.getClass()));
		assertEquals("echo:test", testService.echo("test"));
	}

	@Test
	public void testCircuitBreakerOpenException() {
		assertFalse(echoService instanceof FallbackEchoService);
		assertTrue(Proxy.isProxyClass(echoService.getClass()));
		int errorCount = 0;
		for (int i = 0; i < 50; i++)
			try {
				echoService.echo("test");
			} catch (RemoteAccessException e) {
				errorCount++;
			}
		assertEquals(50, errorCount);
		for (int i = 0; i < 50; i++)
			try {
				echoService.echo("test");
			} catch (RemoteAccessException e) {
				errorCount++;
			}
		assertEquals(100, errorCount);
		// CircuitBreaker is open and fallback will active
		assertEquals("echo:test", echoService.echo("test"));
	}

	@Configuration
	static class RemotingFallbackConfiguration {

		@Bean
		public static RemotingServiceRegistryPostProcessor remotingServiceRegistryPostProcessor() {
			RemotingServiceRegistryPostProcessor obj = new RemotingServiceRegistryPostProcessor();
			obj.setAnnotatedClasses(new Class<?>[] { TestService.class });
			return obj;
		}

		@Bean
		public ServiceRegistry serviceRegistry() {
			return new StandaloneServiceRegistry() {
				@Override
				protected void lookup(String serviceName) {
					// skip lookup force ServiceNotFoundException
				}
			};
		}

		@Bean
		public FallbackTestService fallbackTestService() {
			return new FallbackTestService();
		}

		@Bean
		public HttpInvokerClient echoService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(EchoService.class);
			hic.setHost("localhost");
			hic.setPort(8888);
			return hic;
		}

		@Bean
		public FallbackEchoService fallbackEchoService() {
			return new FallbackEchoService();
		}

		@Bean
		public HttpInvokerRequestExecutor httpInvokerRequestExecutor() {
			return new HttpInvokerRequestExecutor() {
				@Override
				protected RemoteInvocationResult doExecuteRequest(String serviceUrl, MethodInvocation methodInvocation,
						ByteArrayOutputStream baos) throws Exception {
					throw new ConnectException("Connection refused");
				}
			};
		}

	}

	@Fallback
	public static class FallbackTestService extends TestServiceImpl implements TestService {

		@Override
		public String echo(String str) {
			return "echo:" + str;
		}

	}

	public static interface EchoService {
		String echo(String str);
	}

	@Fallback
	public static class FallbackEchoService implements EchoService {

		@Override
		public String echo(String str) {
			return "echo:" + str;
		}

	}

}
