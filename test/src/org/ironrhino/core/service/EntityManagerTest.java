package org.ironrhino.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Gender;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.service.BaseManager.IterateCallback;
import org.ironrhino.core.service.EntityManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "ctx.xml", "/resources/spring/applicationContext-hibernate.xml" })
public class EntityManagerTest {

	@Autowired
	private EntityManager<Person> entityManager;

	@Test
	public void testCrud() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("test");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		entityManager.save(person);
		Person person2 = entityManager.get(person.getId());
		assertEquals(Gender.MALE, person2.getGender());
		person.setGender(Gender.FEMALE);
		entityManager.update(person);
		person2 = entityManager.findByNaturalId("test");
		assertEquals(Gender.FEMALE, person2.getGender());
		person2 = entityManager.findOne("test");
		assertEquals(Gender.FEMALE, person2.getGender());
		person2 = entityManager.findOne("name", "test");
		assertEquals(Gender.FEMALE, person2.getGender());
		entityManager.delete(person2);
		assertNull(entityManager.findByNaturalId("test"));
	}

	@Test
	public void testCriteria() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("test");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		entityManager.save(person);
		assertEquals(1, entityManager.countAll());
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		assertEquals(1, entityManager.countByCriteria(dc));
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("name", "test"));
		Person person2 = entityManager.findByCriteria(dc);
		assertEquals("test", person2.getName());
		entityManager.delete(person);
	}

	@Test
	public void testCriteriaWithList() {
		entityManager.setEntityClass(Person.class);
		int size = 9;
		for (int i = 0; i < size; i++) {
			Person person = new Person();
			person.setName("test" + i);
			person.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
			person.setDateOfBirth(new Date());
			entityManager.save(person);
		}

		List<Person> males = entityManager.findAll(Order.asc("name"));
		assertEquals(size, males.size());

		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		males = entityManager.findListByCriteria(dc);
		assertEquals(5, males.size());

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		males = entityManager.findBetweenListByCriteria(dc, 2, 4);
		assertEquals(2, males.size());
		assertEquals("test4", males.get(0).getName());
		assertEquals("test6", males.get(1).getName());

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		males = entityManager.findListByCriteria(dc, 2, 3);
		assertEquals(2, males.size());
		assertEquals("test6", males.get(0).getName());
		assertEquals("test8", males.get(1).getName());

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		ResultPage<Person> rp = new ResultPage<>();
		rp.setPageNo(2);
		rp.setPageSize(3);
		rp.setCriteria(dc);
		rp = entityManager.findByResultPage(rp);
		males = (List<Person>) rp.getResult();
		assertEquals(2, rp.getTotalPage());
		assertEquals(5, rp.getTotalResults());
		assertEquals(2, males.size());
		assertEquals("test6", males.get(0).getName());
		assertEquals("test8", males.get(1).getName());

		for (int i = 0; i < size; i++) {
			Person person = entityManager.findOne("test" + i);
			entityManager.delete(person);
		}
	}

	@Test
	public void testHql() {
		entityManager.setEntityClass(Person.class);
		int size = 9;
		for (int i = 0; i < size; i++) {
			Person person = new Person();
			person.setName("test" + i);
			person.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
			person.setDateOfBirth(new Date());
			entityManager.save(person);
		}
		List<Person> males = entityManager.find("from Person p where p.gender=?1", Gender.MALE);
		assertEquals(5, males.size());
		entityManager.executeUpdate("delete from Person p where p.gender=?1", Gender.MALE);
		males = entityManager.find("from Person p where p.gender=?1", Gender.MALE);
		assertEquals(0, males.size());
		List<Person> females = entityManager.find("from Person p where p.gender=?1", Gender.FEMALE);
		assertEquals(4, females.size());
		entityManager.executeUpdate("delete from Person p where p.gender=?1", Gender.FEMALE);
		assertEquals(0, entityManager.countAll());
	}

	@Test
	public void testIterate() {
		entityManager.setEntityClass(Person.class);
		int size = 9;
		for (int i = 0; i < size; i++) {
			Person person = new Person();
			person.setName("test" + i);
			person.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
			person.setDateOfBirth(new Date());
			entityManager.save(person);
		}
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		List<Person> males = new ArrayList<>();
		entityManager.iterate(2, new IterateCallback() {
			@Override
			public void process(Object[] entityArray, Session session) {
				for (Object obj : entityArray)
					males.add((Person) obj);
			}
		}, dc);
		assertEquals(5, males.size());
		entityManager.executeUpdate("delete from Person p");
		assertEquals(0, entityManager.countAll());
	}

}
