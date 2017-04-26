package org.ironrhino.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Gender;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.service.BaseManager.IterateCallback;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "ctx.xml" })
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
		assertNotNull(person2.getCreateDate());
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
	public void testLifecyleEvents() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("test");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		entityManager.save(person);
		assertEquals(2, person.lifyCycleEvents.size());
		assertEquals(PrePersist.class.getSimpleName(), person.lifyCycleEvents.get(0));
		assertEquals(PostPersist.class.getSimpleName(), person.lifyCycleEvents.get(1));
		Person person2 = entityManager.get(person.getId());
		assertEquals(1, person2.lifyCycleEvents.size());
		assertEquals(PostLoad.class.getSimpleName(), person2.lifyCycleEvents.get(0));
		person2.setDateOfBirth(new Date());
		entityManager.save(person2);
		assertEquals(3, person2.lifyCycleEvents.size());
		assertEquals(PreUpdate.class.getSimpleName(), person2.lifyCycleEvents.get(1));
		assertEquals(PostUpdate.class.getSimpleName(), person2.lifyCycleEvents.get(2));
		entityManager.delete(person2);
		assertEquals(5, person2.lifyCycleEvents.size());
		assertEquals(PreRemove.class.getSimpleName(), person2.lifyCycleEvents.get(3));
		assertEquals(PostRemove.class.getSimpleName(), person2.lifyCycleEvents.get(4));
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
		prepareData();

		List<Person> males = entityManager.findAll(Order.asc("name"));
		assertEquals(9, males.size());

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

		clearData();
		assertEquals(0, entityManager.countAll());
	}

	@Test
	public void testHql() {
		prepareData();

		List<Person> males = entityManager.find("from Person p where p.gender=?1", Gender.MALE);
		assertEquals(5, males.size());
		entityManager.executeUpdate("delete from Person p where p.gender=?1", Gender.MALE);
		males = entityManager.find("from Person p where p.gender=?1", Gender.MALE);
		assertEquals(0, males.size());
		List<Person> females = entityManager.find("from Person p where p.gender=?1", Gender.FEMALE);
		assertEquals(4, females.size());

		clearData();
		assertEquals(0, entityManager.countAll());
	}

	@Test
	public void testCallback() {
		prepareData();
		Person person = entityManager.executeFind(new HibernateCallback<Person>() {
			@Override
			public Person doInHibernate(Session session) {
				Query q = session.createQuery("from Person p where p.name=:name");
				q.setString("name", "test0");
				q.setMaxResults(1);
				return (Person) q.uniqueResult();
			}
		});
		assertEquals("test0", person.getName());
		clearData();
	}

	@Test
	public void testIterate() {
		prepareData();
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		long count = entityManager.iterate(2, new IterateCallback() {
			@Override
			public void process(Object[] entityArray, Session session) {
				for (Object obj : entityArray) {
					Person p = (Person) obj;
					if (p.getGender() == Gender.MALE) {
						p.setGender(Gender.FEMALE);
						session.update(p);
					}
				}
			}
		}, dc, true);
		assertEquals(5, count);
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.FEMALE));
		dc.addOrder(Order.asc("name"));
		List<Person> males = new ArrayList<>();
		count = entityManager.iterate(2, new IterateCallback() {
			@Override
			public void process(Object[] entityArray, Session session) {
				for (Object obj : entityArray) {
					Person p = (Person) obj;
					if (p.getGender() == Gender.FEMALE) {
						p.setGender(Gender.MALE);
						males.add(p);
						session.update(p);
					}
				}
			}
		}, dc, false);
		assertEquals(9, count);
		assertEquals(9, males.size());
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		count = entityManager.iterate(2, new IterateCallback() {
			@Override
			public void process(Object[] entityArray, Session session) {

			}
		}, dc, true);
		assertEquals(9, count);
		clearData();
	}

	private void prepareData() {
		entityManager.setEntityClass(Person.class);
		int size = 9;
		for (int i = 0; i < size; i++) {
			Person person = new Person();
			person.setName("test" + i);
			person.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
			person.setDateOfBirth(new Date());
			entityManager.save(person);
		}
	}

	private void clearData() {
		entityManager.executeUpdate("delete from Person p");
	}

}
