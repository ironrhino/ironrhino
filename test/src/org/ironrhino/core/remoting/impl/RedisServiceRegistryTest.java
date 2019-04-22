package org.ironrhino.core.remoting.impl;

import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_HOSTS;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_SERVICES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.client.RemotingServiceRegistryPostProcessor;
import org.ironrhino.core.remoting.impl.RedisServiceRegistryTest.RedisServiceRegistryConfiguration;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.FooServiceImpl;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisServiceRegistryConfiguration.class)
@TestPropertySource(properties = "httpInvoker.polling=false")
public class RedisServiceRegistryTest {

	private static boolean firstSetUp = true;

	private static ListOperations<String, String> opsForList;

	private static HashOperations<String, Object, Object> opsForHash;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private AbstractServiceRegistry serviceRegistry;
	@Autowired
	private EventPublisher eventPublisher;
	@Autowired
	private FooService fooService;
	@Autowired
	private HttpInvokerClient barServiceClient;
	@Autowired
	private BarService barService;
	@Autowired
	private HttpInvokerRequestExecutor httpInvokerRequestExecutor;

	private Map<String, List<String>> importedServiceCandidates;
	private Map<String, String> importedServices;
	private Map<String, Object> exportedServices;

	private static String serviceUrl(String host, Class<?> serviceClass) {
		return "http://" + host + "/remoting/httpinvoker/" + serviceClass.getName();
	}

	private static String normalizeHost(String host) {
		int i = host.indexOf('@');
		return i < 0 ? host : host.substring(i + 1);
	}

	private static void setServiceUrl(HttpInvokerClient client, String host) throws Exception {
		Class<?> serviceClass = client.getObjectType();
		if (serviceClass != null) {
			ReflectionUtils.setFieldValue(client, "serviceUrl", serviceUrl(host, serviceClass));
			ReflectionUtils.setFieldValue(client, "discoveredHost", host);
		}
	}

	private static String getServiceUrl(HttpInvokerClient client) throws Exception {
		Field serviceUrlField = HttpInvokerClient.class.getDeclaredField("serviceUrl");
		serviceUrlField.setAccessible(true);
		return (String) serviceUrlField.get(client);
	}

	@PostConstruct
	public void afterPropertiesSet() {
		importedServiceCandidates = serviceRegistry.getImportedServiceCandidates();
		importedServices = serviceRegistry.getImportedServices();
		exportedServices = serviceRegistry.getExportedServices();
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		if (firstSetUp) {
			firstSetUp = false;
			testImportedServiceCandidates();
			testExportedServices();
		}
		given(stringRedisTemplate.opsForList()).willReturn(opsForList = mock(ListOperations.class));
		given(stringRedisTemplate.opsForHash()).willReturn(opsForHash = mock(HashOperations.class));
		importedServiceCandidates.clear();
		importedServices.clear();
		exportedServices.clear();
		clearInvocations(serviceRegistry);
	}

	private void testImportedServiceCandidates() {
		assertTrue(importedServiceCandidates.get(BarService.class.getName())
				.containsAll(Arrays.asList("barService@0.0.0.0:8080", "barService@0.0.0.1:8080")));
	}

	private void testExportedServices() {
		String exportedServiceName = FooService.class.getName();
		assertTrue(exportedServices.containsKey(exportedServiceName));
		assertEquals(fooService, exportedServices.get(exportedServiceName));
		then(opsForList).should().remove(NAMESPACE_SERVICES + exportedServiceName, 0, serviceRegistry.getLocalHost());
		then(opsForList).should().rightPush(NAMESPACE_SERVICES + exportedServiceName, serviceRegistry.getLocalHost());
		then(eventPublisher).should().publish(
				argThat(event -> event instanceof ExportServicesEvent
						&& ((ExportServicesEvent) event).getExportServices().contains(exportedServiceName)),
				eq(Scope.GLOBAL));
	}

	@Test
	public void testRegister() {
		InOrder order = inOrder(opsForList);
		String serviceName = TestService.class.getName();
		serviceRegistry.register(serviceName, new TestServiceImpl());
		assertTrue(exportedServices.containsKey(serviceName));
		then(opsForList).should(order).remove(NAMESPACE_SERVICES + serviceName, 0, serviceRegistry.getLocalHost());
		then(opsForList).should(order).rightPush(NAMESPACE_SERVICES + serviceName, serviceRegistry.getLocalHost());
	}

	@Test
	public void testUnregister() {
		String serviceName = TestService.class.getName();
		exportedServices.put(serviceName, new TestServiceImpl());
		serviceRegistry.unregister(serviceName);
		assertFalse(exportedServices.containsKey(serviceName));
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
		assertNotEquals(host1, host2);
		assertNotEquals(host1, host3);
		assertNotEquals(host2, host3);

		String host4 = serviceRegistry.discover(serviceName, true);
		String host5 = serviceRegistry.discover(serviceName, true);
		String host6 = serviceRegistry.discover(serviceName, true);
		assertEquals(host1, host4);
		assertEquals(host2, host5);
		assertEquals(host3, host6);
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
		assertNotEquals(host1, host2);

		String host3 = serviceRegistry.discover(serviceName, true);
		String host4 = serviceRegistry.discover(serviceName, true);
		assertEquals(host1, host3);
		assertEquals(host2, host4);
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
		assertTrue(serviceCandidates.containsAll(Arrays.asList(provider1, provider2, provider3)));
		assertEquals(normalizeHost(provider3), discoveredHost);
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
		assertEquals(normalizeHost(provider2), discoveredHost);
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
		assertEquals(normalizeHost(provider2), discoveredHost);
		then(opsForHash).should().putAll(eq(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()),
				argThat(importedServices -> importedServices.containsKey(serviceName)
						&& importedServices.get(serviceName).equals(provider2)));
	}

	@Test
	public void testExportedServiceEvent() throws Exception {
		String serviceName = BarService.class.getName();
		String provider1 = "barService@0.0.0.0:8080";
		String provider2 = "barService@0.0.0.1:8080";
		importedServiceCandidates.put(serviceName, new ArrayList<>(Collections.singletonList(provider1)));
		importedServices.put(serviceName, provider1);
		setServiceUrl(barServiceClient, normalizeHost(provider1));

		List<String> hosts = Arrays.asList(serviceRegistry.getLocalHost(), provider1, provider2);
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Set<String>>>any()))
				.willReturn(hosts.stream().map(host -> NAMESPACE_HOSTS + host).collect(Collectors.toSet()));
		given(opsForHash.entries(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList(provider1, provider2));

		ExportServicesEvent event = mock(ExportServicesEvent.class);
		given(event.getInstanceId()).willReturn("barService-CNjk0JtDGt@0.0.0.1:8080");
		given(event.getExportServices()).willReturn(Arrays.asList(serviceName));
		eventPublisher.publish(event, Scope.GLOBAL);

		assertTrue(importedServiceCandidates.get(serviceName).containsAll(Arrays.asList(provider1, provider2)));
		assertEquals(provider2, importedServices.get(serviceName));
		then(serviceRegistry).should().publishServiceHostsChangedEvent(serviceName);
		then(serviceRegistry).should().discover(serviceName, false);
		then(opsForHash).should().putAll(eq(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()),
				argThat(s -> s.containsKey(serviceName) && s.get(serviceName).equals(provider2)));
		assertEquals(serviceUrl(normalizeHost(provider2), BarService.class), getServiceUrl(barServiceClient));
	}

	@Test
	public void testEvict() throws Exception {
		String serviceName = BarService.class.getName();
		String provider1 = "barService@0.0.0.0:8080";
		String provider2 = "barService@0.0.0.1:8080";
		importedServiceCandidates.put(serviceName, new ArrayList<>(Collections.singletonList(provider1)));
		importedServices.put(serviceName, provider1);
		setServiceUrl(barServiceClient, normalizeHost(provider1));

		List<String> hosts = Arrays.asList(serviceRegistry.getLocalHost(), provider1, provider2);
		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Set<String>>>any()))
				.willReturn(hosts.stream().map(host -> NAMESPACE_HOSTS + host).collect(Collectors.toSet()));
		given(opsForHash.entries(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList(provider1, provider2));

		serviceRegistry.evict(provider1);
		then(serviceRegistry).should().onServiceHostsChanged(serviceName);
		then(serviceRegistry).should().publishServiceHostsChangedEvent(serviceName);
		then(serviceRegistry).should().discover(serviceName, false);
		then(opsForHash).should().putAll(eq(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()),
				argThat(s -> s.containsKey(serviceName) && s.get(serviceName).equals(provider2)));
		assertEquals(serviceUrl(normalizeHost(provider2), BarService.class), getServiceUrl(barServiceClient));
	}

	@Test
	public void testEvictWhenRemotingFailed() throws Exception {
		String serviceName = BarService.class.getName();
		String provider1 = "barService@0.0.0.0:8080";
		String provider2 = "barService@0.0.0.1:8080";
		List<String> providerList = Arrays.asList(provider1, provider2);
		importedServiceCandidates.put(serviceName, new ArrayList<>(providerList));
		importedServices.put(serviceName, provider1);
		setServiceUrl(barServiceClient, normalizeHost(provider1));

		List<String> hosts = Arrays.asList(serviceRegistry.getLocalHost(), provider1, provider2);

		given(stringRedisTemplate.execute(ArgumentMatchers.<RedisCallback<Set<String>>>any()))
				.willReturn(hosts.stream().map(host -> NAMESPACE_HOSTS + host).collect(Collectors.toSet()));
		given(opsForHash.entries(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()))
				.willReturn(Collections.singletonMap(serviceName, provider1));
		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1)).willReturn(providerList);

		given(httpInvokerRequestExecutor.getSerializer()).willReturn(HttpInvokerSerializers.DEFAULT_SERIALIZER);
		given(httpInvokerRequestExecutor.executeRequest(eq(serviceUrl(normalizeHost(provider1), BarService.class)),
				any(RemoteInvocation.class), any(MethodInvocation.class)))
						.willThrow(new RuntimeException("intentional error"));
		given(httpInvokerRequestExecutor.executeRequest(eq(serviceUrl(normalizeHost(provider2), BarService.class)),
				any(RemoteInvocation.class), any(MethodInvocation.class)))
						.willReturn(new RemoteInvocationResult("test"));

		assertEquals("test", barService.test(""));
		then(serviceRegistry).should().evict(normalizeHost(provider1));
		then(serviceRegistry).should().onServiceHostsChanged(serviceName);
		then(serviceRegistry).should().publishServiceHostsChangedEvent(serviceName);
		then(serviceRegistry).should().discover(serviceName, false);
		then(opsForHash).should().putAll(eq(NAMESPACE_HOSTS + serviceRegistry.getLocalHost()),
				argThat(s -> s.containsKey(serviceName) && s.get(serviceName).equals(provider2)));
		assertEquals(serviceUrl(normalizeHost(provider2), BarService.class), getServiceUrl(barServiceClient));
	}

	static class RedisServiceRegistryConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		public StringRedisTemplate stringRedisTemplate() {
			StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
			given(stringRedisTemplate.opsForList()).willReturn(opsForList = mock(ListOperations.class));
			given(stringRedisTemplate.opsForHash()).willReturn(opsForHash = mock(HashOperations.class));
			given(opsForList.range(NAMESPACE_SERVICES + BarService.class.getName(), 0, -1))
					.willReturn(Arrays.asList("barService@0.0.0.0:8080", "barService@0.0.0.1:8080"));
			return stringRedisTemplate;
		}

		@Bean
		public RemotingServiceRegistryPostProcessor remotingServiceRegistryPostProcessor() {
			RemotingServiceRegistryPostProcessor registryPostProcessor = new RemotingServiceRegistryPostProcessor();
			registryPostProcessor.setAnnotatedClasses(new Class[] { BarService.class });
			return registryPostProcessor;
		}

		@Bean
		public EventPublisher eventPublisher() {
			return spy(new EventPublisher());
		}

		@Bean
		public RedisServiceRegistry redisServiceRegistry() {
			return spy(new RedisServiceRegistry());
		}

		@Bean
		public HttpInvokerRequestExecutor httpInvokerRequestExecutor() {
			return mock(HttpInvokerRequestExecutor.class);
		}

		@Bean
		public FooServiceImpl fooService() {
			return new FooServiceImpl();
		}
	}

}
