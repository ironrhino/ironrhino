package org.ironrhino.core.remoting;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
import org.ironrhino.core.throttle.CircuitBreakerRegistry;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RemotingFallbackConfiguration.class)
@TestPropertySource(properties = "org.ironrhino.core.remoting.RemotingServiceFallbackTest$EchoService"
		+ HttpInvokerClient.BASE_URL_SUFFIX + "=http://localhost:8888")
public class RemotingServiceFallbackTest {

	@Autowired
	private TestService testService;

	@Autowired
	private EchoService echoService;

	@Test
	public void testServiceNotFoundException() {
		assertThat(testService instanceof FallbackTestService, is(false));
		assertThat(Proxy.isProxyClass(testService.getClass()), is(true));
		assertThat(testService.echo("test"), is("echo:test"));
	}

	@Test
	public void testCircuitBreakerOpenException() {
		assertThat(echoService instanceof FallbackEchoService, is(false));
		assertThat(Proxy.isProxyClass(echoService.getClass()), is(true));
		int errorCount = 0;
		for (int i = 0; i < 50; i++)
			try {
				echoService.echo("test");
			} catch (RemoteAccessException e) {
				errorCount++;
			}
		assertThat(errorCount, is(50));
		for (int i = 0; i < 50; i++)
			try {
				echoService.echo("test");
			} catch (RemoteAccessException e) {
				errorCount++;
			}
		assertThat(errorCount, is(100));
		// CircuitBreaker is open and fallback will active
		assertThat(echoService.echo("test"), is("echo:test"));
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

		@Bean
		public CircuitBreakerRegistry circuitBreakerRegistry() {
			return new CircuitBreakerRegistry();
		}

	}

	@Fallback
	public static class FallbackTestService extends TestServiceImpl implements TestService {

		@Override
		public String echo(String str) {
			return "echo:" + str;
		}

	}

	public interface EchoService {
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
