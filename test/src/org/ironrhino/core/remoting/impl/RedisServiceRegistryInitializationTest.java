package org.ironrhino.core.remoting.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_APPS;
import static org.ironrhino.core.remoting.impl.RedisServiceRegistry.NAMESPACE_SERVICES;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.Arrays;

import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.ExportServicesEvent;
import org.ironrhino.core.remoting.client.RemotingServiceRegistryPostProcessor;
import org.ironrhino.core.remoting.impl.RedisServiceRegistryInitializationTest.RedisServiceRegistryInitialConfiguration;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.FooServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisServiceRegistryInitialConfiguration.class)
@TestPropertySource(properties = { "org.ironrhino.sample.remoting.FooService.description=FooService Test" })
public class RedisServiceRegistryInitializationTest extends RedisServiceRegistryAdapter {

	@Autowired
	private FooService fooService;

	private String exportedServiceName = FooService.class.getName();

	@Test
	public void testInitialize() throws Exception {
		testImportedServiceCandidates();
		testExportedServices();
		testExportedServiceDescriptions();
		testExportServiceEvent();
	}

	private void testImportedServiceCandidates() {
		assertThat(importedServiceCandidates.get(BarService.class.getName())
				.containsAll(Arrays.asList("barService@0.0.0.0:8080", "barService@0.0.0.1:8080")), is(true));
	}

	private void testExportedServices() {
		assertThat(exportedServices.containsKey(exportedServiceName), is(true));
		assertThat(exportedServices.get(exportedServiceName), is(fooService));
		then(opsForList).should().remove(NAMESPACE_SERVICES + exportedServiceName, 0, serviceRegistry.getLocalHost());
		then(opsForList).should().rightPush(NAMESPACE_SERVICES + exportedServiceName, serviceRegistry.getLocalHost());
	}

	private void testExportedServiceDescriptions() {
		String exportedServiceName = FooService.class.getName();
		assertThat(exportedServiceDescriptions.containsKey(exportedServiceName), is(true));
		assertThat(exportedServiceDescriptions.get(exportedServiceName), is("FooService Test"));
		then(opsForHash).should().putAll(NAMESPACE_APPS + AppInfo.getAppName(), exportedServiceDescriptions);
	}

	private void testExportServiceEvent() {
		then(eventPublisher).should().publish(
				argThat(event -> event instanceof ExportServicesEvent
						&& ((ExportServicesEvent) event).getExportServices().contains(exportedServiceName)),
				eq(Scope.GLOBAL));
	}

	static class RedisServiceRegistryInitialConfiguration {

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
		public FooServiceImpl fooService() {
			return new FooServiceImpl();
		}
	}
}
