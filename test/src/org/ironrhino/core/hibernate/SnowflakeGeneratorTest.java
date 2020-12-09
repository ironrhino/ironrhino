package org.ironrhino.core.hibernate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.criterion.Order;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.service.HibernateConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HibernateConfiguration.class)
@TestPropertySource(properties = {
		"annotatedClasses=org.ironrhino.core.hibernate.LongIdMessage,org.ironrhino.core.hibernate.StringIdMessage" })
public class SnowflakeGeneratorTest {

	@Autowired
	private EntityManager<LongIdMessage> longIdMessageManager;

	@Autowired
	private EntityManager<StringIdMessage> stringIdMessageManager;

	@Test
	public void testLongId() throws Exception {
		longIdMessageManager.setEntityClass(LongIdMessage.class);
		int size = 100;
		for (int i = 0; i < size; i++) {
			LongIdMessage message = new LongIdMessage();
			message.setTitle("title" + i);
			longIdMessageManager.save(message);
			TimeUnit.MILLISECONDS.sleep(20);
		}
		List<LongIdMessage> list = longIdMessageManager.findAll(Order.asc("id"));
		assertThat(list.size(), equalTo(size));
		for (int i = 0; i < size; i++) {
			assertThat(list.get(i).getTitle(), equalTo("title" + i));
		}
		list = longIdMessageManager.findAll(Order.desc("id"));
		assertThat(list.size(), equalTo(size));
		for (int i = 0; i < size; i++) {
			assertThat(list.get(i).getTitle(), equalTo("title" + (size - i - 1)));
		}
		longIdMessageManager.executeUpdate("delete from LongIdMessage");
	}

	@Test
	public void testStringId() throws Exception {
		stringIdMessageManager.setEntityClass(StringIdMessage.class);
		int size = 100;
		for (int i = 0; i < size; i++) {
			StringIdMessage message = new StringIdMessage();
			message.setTitle("title" + i);
			stringIdMessageManager.save(message);
			TimeUnit.MILLISECONDS.sleep(20);
		}
		List<StringIdMessage> list = stringIdMessageManager.findAll(Order.asc("id"));
		assertThat(list.size(), equalTo(size));
		for (int i = 0; i < size; i++) {
			assertThat(list.get(i).getTitle(), equalTo("title" + i));
		}
		list = stringIdMessageManager.findAll(Order.desc("id"));
		assertThat(list.size(), equalTo(size));
		for (int i = 0; i < size; i++) {
			assertThat(list.get(i).getTitle(), equalTo("title" + (size - i - 1)));
		}
		stringIdMessageManager.executeUpdate("delete from StringIdMessage");
	}

}
