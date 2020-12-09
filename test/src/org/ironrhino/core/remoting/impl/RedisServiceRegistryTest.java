package org.ironrhino.core.remoting.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_SERVICES;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisServiceRegistryTest.Config.class)
public class RedisServiceRegistryTest extends RedisServiceRegistryAdapter {

	@Before
	@SuppressWarnings("unchecked")
	public void clear() throws Exception {
		given(stringRedisTemplate.opsForList()).willReturn(opsForList = mock(ListOperations.class));
		given(stringRedisTemplate.opsForHash()).willReturn(opsForHash = mock(HashOperations.class));
		importedServiceCandidates.clear();
		exportedServices.clear();
		clearInvocations(serviceRegistry);
	}

	@Test
	public void testRegister() {
		InOrder order = inOrder(opsForList);
		String serviceName = TestService.class.getName();
		serviceRegistry.register(serviceName, new TestServiceImpl());
		assertThat(exportedServices.containsKey(serviceName), is(true));
		then(opsForList).should(order).remove(NAMESPACE_SERVICES + serviceName, 0, serviceRegistry.getLocalHost());
		then(opsForList).should(order).rightPush(NAMESPACE_SERVICES + serviceName, serviceRegistry.getLocalHost());
	}

	@Test
	public void testUnregister() {
		String serviceName = TestService.class.getName();
		exportedServices.put(serviceName, new TestServiceImpl());
		serviceRegistry.unregister(serviceName);
		assertThat(exportedServices.containsKey(serviceName), is(false));
		then(opsForList).should().remove(NAMESPACE_SERVICES + serviceName, 0, serviceRegistry.getLocalHost());
	}

	@Test
	public void testDiscoverByPolling() {
		String serviceName = BarService.class.getName();
		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList("node@0.0.0.0:8080", "node@0.0.0.0:8081", "node@0.0.0.0:8082"));

		String host1 = serviceRegistry.discover(serviceName);
		String host2 = serviceRegistry.discover(serviceName);
		String host3 = serviceRegistry.discover(serviceName);
		assertThat(host2, is(not(host1)));
		assertThat(host3, is(not(host1)));
		assertThat(host3, is(not(host2)));

		String host4 = serviceRegistry.discover(serviceName);
		String host5 = serviceRegistry.discover(serviceName);
		String host6 = serviceRegistry.discover(serviceName);
		assertThat(host4, is(host1));
		assertThat(host5, is(host2));
		assertThat(host6, is(host3));
	}

	@Test
	public void testDiscoverFromNearestByPolling() {
		String serviceName = BarService.class.getName();
		String nearestHost1 = AppInfo.getHostAddress() + ":8081";
		String nearestHost2 = AppInfo.getHostAddress() + ":8082";
		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1)).willReturn(Arrays.asList("node@0.0.0.0:8080",
				"node@0.0.0.0:8081", "node@" + nearestHost1, "node@" + nearestHost2));

		String host1 = serviceRegistry.discover(serviceName);
		String host2 = serviceRegistry.discover(serviceName);
		assertThat(host2, is(not(host1)));

		String host3 = serviceRegistry.discover(serviceName);
		String host4 = serviceRegistry.discover(serviceName);
		assertThat(host3, is(host1));
		assertThat(host4, is(host2));
	}

	static class Config {

		@Bean
		public StringRedisTemplate stringRedisTemplate() {
			return mock(StringRedisTemplate.class);
		}

		@Bean
		public EventPublisher eventPublisher() {
			return spy(new EventPublisher());
		}

		@Bean
		public RedisServiceRegistry redisServiceRegistry() {
			return spy(new RedisServiceRegistry());
		}
	}
}
