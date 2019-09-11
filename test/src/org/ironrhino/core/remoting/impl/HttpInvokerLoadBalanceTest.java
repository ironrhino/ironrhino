package org.ironrhino.core.remoting.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_SERVICES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.client.RemotingServiceRegistryPostProcessor;
import org.ironrhino.core.remoting.impl.HttpInvokerLoadBalanceTest.HttpInvokerConfiguration;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.sample.remoting.BarService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HttpInvokerConfiguration.class)
public class HttpInvokerLoadBalanceTest extends RedisServiceRegistryAdapter {

	@Autowired
	protected BarService barService;
	@Autowired
	protected HttpInvokerClient barServiceClient;
	@Autowired
	protected HttpInvokerRequestExecutor httpInvokerRequestExecutor;

	@Before
	@SuppressWarnings("unchecked")
	public void clear() throws Exception {
		given(stringRedisTemplate.opsForList()).willReturn(opsForList = mock(ListOperations.class));
		given(stringRedisTemplate.opsForHash()).willReturn(opsForHash = mock(HashOperations.class));
		importedServiceCandidates.clear();
		exportedServices.clear();
		clearInvocations(serviceRegistry);
	}

	protected static String serviceUrl(String host, Class<?> serviceClass) {
		return "http://" + host + "/remoting/httpinvoker/" + serviceClass.getName();
	}

	protected static void setServiceUrl(HttpInvokerClient client, String host) throws Exception {
		Class<?> serviceClass = client.getObjectType();
		if (serviceClass != null) {
			ReflectionUtils.setFieldValue(client, "serviceUrl", serviceUrl(host, serviceClass));
			ReflectionUtils.setFieldValue(client, "discoveredHost", host);
		}
	}

	protected static String getServiceUrl(HttpInvokerClient client) throws Exception {
		return ReflectionUtils.getFieldValue(client, "serviceUrl");
	}

	@Test
	public void testExportedServiceEvent() throws Exception {
		String serviceName = BarService.class.getName();
		String provider1 = "barService@0.0.0.0:8080";
		String provider2 = "barService@0.0.0.1:8080";
		importedServiceCandidates.put(serviceName, new ArrayList<>(Collections.singletonList(provider1)));
		setServiceUrl(barServiceClient, normalizeHost(provider1));

		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList(provider1, provider2));

		ExportServicesEvent event = mock(ExportServicesEvent.class);
		given(event.getInstanceId()).willReturn("barService-CNjk0JtDGt@0.0.0.1:8080");
		given(event.getExportServices()).willReturn(Arrays.asList(serviceName));
		eventPublisher.publish(event, Scope.GLOBAL);

		assertThat(importedServiceCandidates.get(serviceName).containsAll(Arrays.asList(provider1, provider2)),
				is(true));
		assertThat(getServiceUrl(barServiceClient), is(serviceUrl(normalizeHost(provider1), BarService.class)));
	}

	@Test
	public void testEvict() throws Exception {
		String serviceName = BarService.class.getName();
		String provider1 = "barService@0.0.0.0:8080";
		String provider2 = "barService@0.0.0.1:8080";
		importedServiceCandidates.put(serviceName, new ArrayList<>(Collections.singletonList(provider1)));
		setServiceUrl(barServiceClient, normalizeHost(provider1));

		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1))
				.willReturn(Arrays.asList(provider1, provider2));

		serviceRegistry.evict(provider1);

		assertThat(importedServiceCandidates.get(serviceName).isEmpty(), is(true));
		assertThat(getServiceUrl(barServiceClient), is(serviceUrl(normalizeHost(provider1), BarService.class)));
	}

	@Test
	public void testEvictWhenRemotingFailed() throws Exception {
		String serviceName = BarService.class.getName();
		String provider1 = "barService@0.0.0.0:8080";
		String provider2 = "barService@0.0.0.1:8080";
		List<String> providerList = Arrays.asList(provider1, provider2);
		importedServiceCandidates.put(serviceName, new ArrayList<>(providerList));
		setServiceUrl(barServiceClient, normalizeHost(provider1));

		given(opsForList.range(NAMESPACE_SERVICES + serviceName, 0, -1)).willReturn(providerList);
		given(httpInvokerRequestExecutor.getSerializer()).willReturn(HttpInvokerSerializers.DEFAULT_SERIALIZER);
		given(httpInvokerRequestExecutor.executeRequest(eq(serviceUrl(normalizeHost(provider1), BarService.class)),
				any(RemoteInvocation.class), any(MethodInvocation.class)))
						.willThrow(new RuntimeException("intentional error"));
		given(httpInvokerRequestExecutor.executeRequest(eq(serviceUrl(normalizeHost(provider2), BarService.class)),
				any(RemoteInvocation.class), any(MethodInvocation.class)))
						.willReturn(new RemoteInvocationResult("test"));

		assertThat(barService.test(""), is("test"));
		int discoverTimes = 2;
		try {
			then(serviceRegistry).should().evict(normalizeHost(provider1));
		} catch (Throwable e) {
			assertThat(barService.test(""), is("test"));
			then(serviceRegistry).should().evict(normalizeHost(provider1));
			discoverTimes = 3;
		}
		then(serviceRegistry).should(times(discoverTimes)).discover(serviceName);
		assertThat(getServiceUrl(barServiceClient), is(serviceUrl(normalizeHost(provider2), BarService.class)));
	}

	static class HttpInvokerConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		public StringRedisTemplate stringRedisTemplate() {
			StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
			given(stringRedisTemplate.opsForList()).willReturn(opsForList = mock(ListOperations.class));
			given(stringRedisTemplate.opsForHash()).willReturn(opsForHash = mock(HashOperations.class));
			return stringRedisTemplate;
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
		public RemotingServiceRegistryPostProcessor remotingServiceRegistryPostProcessor() {
			RemotingServiceRegistryPostProcessor registryPostProcessor = new RemotingServiceRegistryPostProcessor();
			registryPostProcessor.setAnnotatedClasses(new Class[] { BarService.class });
			return registryPostProcessor;
		}
	}
}
