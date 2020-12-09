package org.ironrhino.common.record;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.service.HibernateConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RecordAspectTest.Config.class)
@TestPropertySource(properties = {
		"annotatedClasses=org.ironrhino.common.record.Record,org.ironrhino.common.record.TestEntity" })
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RecordAspectTest {

	@Autowired
	private EntityManager entityManager;

	@Test
	public void test() {
		TestEntity entity = new TestEntity();
		entity.setName("test");
		entityManager.save(entity);
		entityManager.setEntityClass(Record.class);
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.addOrder(Order.desc("recordDate"));
		Record record = (Record) entityManager.findByCriteria(dc);
		assertThat(record.getEntityId(), is(entity.getId()));
		assertThat(record.getEntityClass(), is(entity.getClass().getName()));
		assertThat(record.getAction(), is("CREATE"));
		assertThat(record.getEntityToString(), is("name: test"));

		entityManager.execute(session -> {
			TestEntity entity2 = session.get(TestEntity.class, entity.getId());
			entity2.setName("test2");
			return null;
		});
		record = (Record) entityManager.findByCriteria(dc);
		assertThat(record.getEntityId(), is(entity.getId()));
		assertThat(record.getEntityClass(), is(entity.getClass().getName()));
		assertThat(record.getAction(), is("UPDATE"));
		assertThat(record.getEntityToString(), is("name: test -> test2"));

		entityManager.delete(entity);
		record = (Record) entityManager.findByCriteria(dc);
		assertThat(record.getEntityId(), is(entity.getId()));
		assertThat(record.getEntityClass(), is(entity.getClass().getName()));
		assertThat(record.getAction(), is("DELETE"));

	}

	@Configuration
	static class Config extends HibernateConfiguration {

		@Bean
		public EventListenerForRecord eventListenerForRecord() {
			return new EventListenerForRecord();
		}

		@Bean
		public RecordAspect recordAspect() {
			return new RecordAspect();
		}

	}

}
