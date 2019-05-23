package org.ironrhino.core.remoting.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_HOSTS;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_SERVICES;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.remoting.impl.RedisServiceRegistryTest.RedisServiceRegistryConfiguration;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisServiceRegistryConfiguration.class)
public class RedisServiceRegistryTest extends RedisServiceRegistryAdapter {

	@Before
	@SuppressWarnings("unchecked")
	public void clear() throws Exception {
		given(stringRedisTemplate.opsForList()).willReturn(opsForList = mock(ListOperations.class));
		given(stringRedisTemplate.opsForHash()).willReturn(opsForHash = mock(HashOperations.class));
		importedServiceCandidates.clear();
		importedServices.clear();
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

		String host1 = serviceRegistry.discover(serviceName, true);
		String host2 = serviceRegistry.discover(serviceName, true);
		String host3 = serviceRegistry.discover(serviceName, true);
		assertThat(host2, is(not(host1)));
		assertThat(host3, is(not(host1)));
		assertThat(host3, is(not(host2)));

		String host4 = serviceRegistry.discover(serviceName, true);
		String host5 = serviceRegistry.discover(serviceName, true);
		String host6 = serviceRegistry.discover(serviceName, true);
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

		String host1 = serviceRegistry.discover(serviceName, true);
		String host2 = serviceRegistry.discover(serviceName, true);
		assertThat(host2, is(not(host1)));

		String host3 = serviceRegistry.discover(serviceName, true);
		String host4 = serviceRegistry.discover(serviceName, true);
		assertThat(host3, is(host1));
		assertThat(host4, is(host2));
	}

	@Test
	public void testDiscoverByLoadBalancing() {
		String serviceName = BarService.class.getName();
		String provider1 = "node@0.0.0.0:8080";
		String provider2 = "node@0.0.0.0:8081";
		String provider3 = "node@0.0.0.0:8082";

		List<String> hosts = Arrays.asList(provider1, provider2, provider3, "node1@0.0.0.1:8080", "node1@0.0.0.1:8081",
				"node1@0.0.0.1:8082", "node2@0.0.0.2:8080", "node2@0.0.0.2:8081", "node2@0.0.0.2:8082",
				"node3@0.0.0.3:8080", "node3@0.0.0.3:8081", "node3@0.0.0.3:80802");
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Set<String>>>any()))
				.willReturn(hosts.stream().map(s -> NAMESPACE_HOSTS + s).collect(Collectors.toSet()));
		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList(provider1, provider2, provider3));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node1@0.0.0.1:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node1@0.0.0.1:8081"))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node1@0.0.0.1:8082"))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node2@0.0.0.2:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider2));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node2@0.0.0.2:8081"))
				.willReturn(Collections.singletonMap(serviceName, provider2));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node2@0.0.0.2:8082"))
				.willReturn(Collections.singletonMap(serviceName, provider3));

		String discoveredHost = serviceRegistry.discover(serviceName, false);
		List<String> serviceCandidates = importedServiceCandidates.get(serviceName);
		assertThat(serviceCandidates.containsAll(Arrays.asList(provider1, provider2, provider3)), is(true));
		assertThat(discoveredHost, is(normalizeHost(provider3)));
		then(opsForHash).should().putAll(eq(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()),
				argThat(importedServices -> importedServices.containsKey(serviceName)
						&& provider3.equals(importedServices.get(serviceName))));

		given(opsForHash.entries(NAMESPACE_HOSTS + "node3@0.0.0.3:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider3));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node3@0.0.0.3:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider3));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node3@0.0.0.3:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider3));

		discoveredHost = serviceRegistry.discover(serviceName, false);
		assertThat(discoveredHost, is(normalizeHost(provider2)));
	}

	@Test
	public void testDiscoverFromNearestByLoadBalancing() {
		String serviceName = BarService.class.getName();
		String provider1 = "node@" + AppInfo.getHostAddress() + ":8081";
		String provider2 = "node@" + AppInfo.getHostAddress() + ":8082";
		String provider3 = "node@0.0.0.0:8083";
		List<String> hosts = Arrays.asList(provider1, provider2, provider3, "node1@0.0.0.1:8080", "node1@0.0.0.1:8081",
				"node1@0.0.0.1:8082", "node2@0.0.0.2:8080", "node2@0.0.0.2:8081", "node2@0.0.0.2:8082");

		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList(provider1, provider2, provider3));
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Set<String>>>any()))
				.willReturn(hosts.stream().map(s -> NAMESPACE_HOSTS + s).collect(Collectors.toSet()));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node1@0.0.0.1:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node1@0.0.0.1:8081"))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node1@0.0.0.1:8082"))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node2@0.0.0.2:8080"))
				.willReturn(Collections.singletonMap(serviceName, provider2));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node2@0.0.0.2:8081"))
				.willReturn(Collections.singletonMap(serviceName, provider2));
		given(opsForHash.entries(NAMESPACE_HOSTS + "node2@0.0.0.2:8082"))
				.willReturn(Collections.singletonMap(serviceName, provider3));

		String discoveredHost = serviceRegistry.discover(serviceName, false);
		assertThat(discoveredHost, is(normalizeHost(provider2)));
		then(opsForHash).should().putAll(eq(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()),
				argThat(importedServices -> importedServices.containsKey(serviceName)
						&& importedServices.get(serviceName).equals(provider2)));
	}

	static class RedisServiceRegistryConfiguration {

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
