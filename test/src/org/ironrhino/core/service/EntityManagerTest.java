package org.ironrhino.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.validation.ConstraintViolationException;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.Transformers;
import org.ironrhino.common.model.Gender;
import org.ironrhino.core.model.ResultPage;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.Data;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HibernateConfiguration.class)
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.service.Person", "hibernate.show_sql=true" })
public class EntityManagerTest {

	@Autowired
	private EntityManager<Person> entityManager;

	@Test
	public void testCrud() {
		entityManager.setEntityClass(Person.class);
		assertNull(entityManager.get("notexistsid"));
		assertNull(entityManager.findByNaturalId("notexistsid"));
		assertNull(entityManager.findOne("notexistsid"));
		Person person = new Person();
		person.setName("test");
		person.setCode("9527");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		entityManager.save(person);

		Person person2 = entityManager.get(person.getId());
		assertEquals(Gender.MALE, person2.getGender());
		assertNotNull(person2.getCreateDate());
		person.setGender(Gender.FEMALE);
		entityManager.update(person);

		assertTrue(entityManager.existsNaturalId("test"));
		person2 = entityManager.findByNaturalId("test");
		assertEquals(Gender.FEMALE, person2.getGender());
		assertTrue(entityManager.existsNaturalId("name", "test"));
		person2 = entityManager.findByNaturalId("name", "test");
		assertEquals(Gender.FEMALE, person2.getGender());

		try {
			entityManager.existsNaturalId("code", "9527");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		try {
			entityManager.findByNaturalId("code", "9527");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		person2 = entityManager.findOne("test");
		assertEquals(Gender.FEMALE, person2.getGender());
		person2 = entityManager.findOne(true, new Serializable[] { "name", "test" });
		assertEquals(Gender.FEMALE, person2.getGender());
		assertTrue(entityManager.existsOne("name", "test"));
		assertTrue(entityManager.existsOne(true, new Serializable[] { "name", "Test" }));
		assertFalse(entityManager.existsOne(true, new Serializable[] { "name", null }));
		person2 = entityManager.findOne("name", "test");
		assertEquals(Gender.FEMALE, person2.getGender());

		person2 = entityManager.findOne(true, new Serializable[] { "code", "9527" });
		assertEquals(Gender.FEMALE, person2.getGender());
		assertTrue(entityManager.existsOne("code", "9527"));
		assertTrue(entityManager.existsOne(true, new Serializable[] { "code", "9527" }));
		person2 = entityManager.findOne("code", "9527");
		assertEquals(Gender.FEMALE, person2.getGender());

		assertTrue(entityManager.exists(person.getId()));
		entityManager.delete(person2);
		assertFalse(entityManager.exists(person.getId()));
		assertNull(entityManager.findByNaturalId("test"));

	}

	@Test(expected = ObjectOptimisticLockingFailureException.class)
	public void testVersion() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("test");
		person.setCode("9527");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		assertEquals(-1, person.getVersion());
		entityManager.save(person);
		assertEquals(0, person.getVersion());
		person.setCreateDate(new Date());
		entityManager.save(person);
		assertEquals(1, person.getVersion());
		entityManager.save(person);
		assertEquals(2, person.getVersion());
		entityManager.execute(session -> {
			Person p = session.get(Person.class, person.getId());
			assertEquals(2, p.getVersion());
			p.setVersion(p.getVersion() - 1);
			session.update(p);
			return null;
		});
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

		prepareData();

		Map<String, Object> args = new HashMap<>();
		args.put("gender", Gender.MALE);
		males = entityManager.find("from Person p where p.gender=:gender", args);
		assertEquals(5, males.size());
		entityManager.executeUpdate("delete from Person p where p.gender=:gender", args);
		males = entityManager.find("from Person p where p.gender=:gender", args);
		assertEquals(0, males.size());
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Test
	public void testResultTransformer() {
		prepareData();
		String hql = "select p.name as name,p.code as code,p.gender as gender from Person p where p.gender=:gender order by p.name";
		List<PersonDTO> males = entityManager.executeFind(session -> {
			return session.createQuery(hql).setResultTransformer(Transformers.aliasToBean(PersonDTO.class))
					.setParameter("gender", Gender.MALE).list();
		});
		assertEquals(5, males.size());
		assertEquals("test0", males.get(0).getName());
		assertEquals("code0", males.get(0).getCode());
		assertEquals(Gender.MALE, males.get(0).getGender());
		List<Map<String, Object>> males2 = entityManager.executeFind(session -> {
			return session.createQuery(hql).setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
					.setParameter("gender", Gender.MALE).list();
		});
		assertEquals(5, males2.size());
		assertEquals("test0", males2.get(0).get("name"));
		assertEquals("code0", males2.get(0).get("code"));
		assertEquals(Gender.MALE, males2.get(0).get("gender"));
		List<Object[]> males3 = entityManager.executeFind(session -> {
			return session.createQuery(hql).setParameter("gender", Gender.MALE).list();
		});
		assertEquals(5, males3.size());
		assertEquals("test0", males3.get(0)[0]);
		assertEquals("code0", males3.get(0)[1]);
		assertEquals(Gender.MALE, males3.get(0)[2]);
		List<PersonDTO> males4 = entityManager.executeFind(session -> {
			DetachedCriteria dc = DetachedCriteria.forClass(Person.class);
			dc.setProjection(Projections.projectionList().add(Projections.property("name"), "name")
					.add(Projections.property("code"), "code").add(Projections.property("gender"), "gender"))
					.setResultTransformer(Transformers.aliasToBean(PersonDTO.class));
			dc.add(Restrictions.eq("gender", Gender.MALE));
			return dc.getExecutableCriteria(session).list();
		});
		assertEquals(5, males4.size());
		assertEquals("test0", males4.get(0).getName());
		assertEquals("code0", males4.get(0).getCode());
		assertEquals(Gender.MALE, males4.get(0).getGender());
	}

	@Test
	public void testCallback() {
		prepareData();
		Person person = entityManager.executeFind(session -> {
			Query<Person> q = session.createQuery("from Person p where p.name=:name", Person.class);
			q.setParameter("name", "test0");
			q.setMaxResults(1);
			return q.uniqueResult();
		});
		assertEquals("test0", person.getName());
	}

	@Test
	public void testIterate() {
		prepareData();

		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		AtomicInteger ai = new AtomicInteger();
		long count = entityManager.iterate(2, (entities, session) -> {
			for (Person p : entities) {
				if (p.getGender() == Gender.MALE) {
					p.setGender(Gender.FEMALE);
					session.update(p);
				}
			}
		}, people -> ai.getAndAdd(people.length), dc);
		assertEquals(5, count);
		assertEquals(5, ai.get());

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.FEMALE));
		dc.addOrder(Order.asc("name"));
		count = entityManager.iterate(2, (entities, session) -> {
			for (Person p : entities) {
				if (p.getGender() == Gender.FEMALE) {
					p.setGender(Gender.MALE);
					session.update(p);
				}
			}
		}, dc, true);
		assertEquals(9, count);

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		try {
			count = entityManager.iterate(2, (entities, session) -> {
				for (Person p : entities) {
					p.setGender(Gender.FEMALE);
					session.update(p);
					if (p.getName().equals("test5"))
						throw new RuntimeException("for test");
				}
			}, dc, true);
		} catch (RuntimeException e) {

		}
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.FEMALE));
		assertEquals(2 * 2, entityManager.countByCriteria(dc)); // first 2 batch

		entityManager.executeUpdate("update Person set gender=?1", Gender.FEMALE);

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.FEMALE));
		dc.addOrder(Order.asc("name"));
		List<Person> males = new ArrayList<>();
		count = entityManager.iterate(2, (entities, session) -> {
			for (Person p : entities) {
				if (p.getGender() == Gender.FEMALE) {
					p.setGender(Gender.MALE);
					males.add(p);
					session.update(p);
				}
			}
		}, dc, true);
		assertEquals(9, count);
		assertEquals(9, males.size());

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		count = entityManager.iterate(2, (entities, session) -> {
		}, dc, true);
		assertEquals(9, count);

	}

	@Test(expected = ConstraintViolationException.class)
	public void testValidation() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("");
		entityManager.save(person);
	}

	private void prepareData() {
		entityManager.setEntityClass(Person.class);
		int size = 9;
		for (int i = 0; i < size; i++) {
			Person person = new Person();
			person.setName("test" + i);
			person.setCode("code" + i);
			person.setGender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE);
			person.setDateOfBirth(new Date());
			entityManager.save(person);
		}
	}

	@After
	public void clearData() {
		entityManager.executeUpdate("delete from Person p");
	}

	@Data
	public static class PersonDTO {

		private String name;

		private String code;

		private Gender gender;

	}

}
