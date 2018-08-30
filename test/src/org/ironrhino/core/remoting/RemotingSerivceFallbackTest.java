package org.ironrhino.core.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import org.ironrhino.core.remoting.RemotingSerivceFallbackTest.RemotingFallbackConfiguration;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RemotingFallbackConfiguration.class)
public class RemotingSerivceFallbackTest {

	@Autowired
	private TestService testService;

	@Test
	public void test() {
		assertFalse(testService instanceof FallbackTestService);
		assertTrue(Proxy.isProxyClass(testService.getClass()));
		// ServiceNotFoundException
		assertEquals("echo:test", testService.echo("test"));
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
			return new StandaloneServiceRegistry();
		}

		@Bean
		public FallbackTestService fallbackTestService() {
			return new FallbackTestService();
		}

	}

	@Fallback
	static class FallbackTestService extends TestServiceImpl implements TestService {

		@Override
		public String echo(String str) {
			return "echo:" + str;
		}

	}

}
