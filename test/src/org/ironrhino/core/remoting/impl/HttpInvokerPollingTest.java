package org.ironrhino.core.remoting.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_HOSTS;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_SERVICES;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.sample.remoting.BarService;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "httpInvoker.polling=true")
public class HttpInvokerPollingTest extends HttpInvokerLoadBalanceTest {

	@Test
	@Override
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

		then(serviceRegistry).should().onServiceHostsChanged(serviceName);
		then(serviceRegistry).should().publishServiceHostsChangedEvent(serviceName);
		then(opsForHash).should().delete(NAMESPACE_HOSTS + serviceRegistry.getLocalHost(), serviceName);

		assertThat(importedServices.get(serviceName), is(nullValue()));
		assertThat(importedServiceCandidates.get(serviceName).containsAll(Arrays.asList(provider1, provider2)),
				is(true));
		assertThat(getServiceUrl(barServiceClient), is(serviceUrl(normalizeHost(provider1), BarService.class)));
	}

	@Test
	@Override
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
		then(opsForHash).should().delete(NAMESPACE_HOSTS + serviceRegistry.getLocalHost(), serviceName);

		assertThat(importedServices.get(serviceName), is(nullValue()));
		assertThat(importedServiceCandidates.get(serviceName).isEmpty(), is(true));
		assertThat(getServiceUrl(barServiceClient), is(serviceUrl(normalizeHost(provider1), BarService.class)));
	}

	@Test
	@Override
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

		assertThat(barService.test(""), is("test"));
		int discoverTimes = 2;
		try {
			then(serviceRegistry).should().evict(normalizeHost(provider1));
		} catch (Throwable e) {
			assertThat(barService.test(""), is("test"));
			then(serviceRegistry).should().evict(normalizeHost(provider1));
			discoverTimes = 3;
		}
		then(serviceRegistry).should().onServiceHostsChanged(serviceName);
		then(serviceRegistry).should().publishServiceHostsChangedEvent(serviceName);
		then(opsForHash).should().delete(NAMESPACE_HOSTS + serviceRegistry.getLocalHost(), serviceName);
		then(serviceRegistry).should(times(discoverTimes)).discover(serviceName, true);
		assertThat(getServiceUrl(barServiceClient), is(serviceUrl(normalizeHost(provider2), BarService.class)));
	}
}
