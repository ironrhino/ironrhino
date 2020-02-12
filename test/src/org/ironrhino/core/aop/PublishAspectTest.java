package org.ironrhino.core.aop;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.ironrhino.core.aop.PublishAspectTest.PublishAspectConfiguration;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.service.HibernateConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PublishAspectConfiguration.class)
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.aop.TestEntity" })
public class PublishAspectTest {

	@Autowired
	private EntityManager<TestEntity> entityManager;

	@Autowired
	private TestEventListener testEventListener;

	@Test
	public void test() {
		TestEntity entity = new TestEntity();
		entity.setName("test");
		entityManager.save(entity);
		assertThat(testEventListener.events.size(), is(1));
		EntityOperationEvent<TestEntity> event = testEventListener.events.get(0);
		assertThat(event.getEntity(), is(entity));
		assertThat(event.getType(), is(EntityOperationType.CREATE));
		entity.setName("test2");
		entityManager.save(entity);
		assertThat(testEventListener.events.size(), is(2));
		event = testEventListener.events.get(1);
		assertThat(event.getEntity(), is(entity));
		assertThat(event.getType(), is(EntityOperationType.UPDATE));
		entityManager.delete(entity);
		assertThat(testEventListener.events.size(), is(3));
		event = testEventListener.events.get(2);
		assertThat(event.getEntity(), is(entity));
		assertThat(event.getType(), is(EntityOperationType.DELETE));
	}

	static class TestEventListener {

		List<EntityOperationEvent<TestEntity>> events = new ArrayList<>();

		@EventListener
		public void onApplicationEvent(EntityOperationEvent<TestEntity> event) {
			events.add(event);
		}
	}

	@Configuration
	static class PublishAspectConfiguration extends HibernateConfiguration {

		@Bean
		public EventPublisher eventPublisher() {
			return new EventPublisher();
		}

		@Bean
		public EventListenerForPublish eventListenerForPublish() {
			return new EventListenerForPublish();
		}

		@Bean
		public PublishAspect publishAspect() {
			return new PublishAspect();
		}

		@Bean
		public TestEventListener testEventListener() {
			return new TestEventListener();
		}

	}

}
