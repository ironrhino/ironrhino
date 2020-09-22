package org.ironrhino.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.time.YearMonth;
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
import org.hibernate.query.NativeQuery;
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
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.service.Person,org.ironrhino.core.service.Cat", "hibernate.show_sql=true" })
public class EntityManagerTest {

	@Autowired
	private EntityManager<Person> entityManager;

	@Autowired
	private EntityManager<Cat> catManager;

	@Test
	public void testCrud() {
		entityManager.setEntityClass(Person.class);
		assertThat(entityManager.get("notexistsid"), is(nullValue()));
		assertThat(entityManager.findByNaturalId("notexistsid"), is(nullValue()));
		assertThat(entityManager.findOne("notexistsid"), is(nullValue()));
		Person person = new Person();
		person.setName("test");
		person.setCode("9527");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		entityManager.save(person);

		Person person2 = entityManager.get(person.getId());
		assertThat(person2.getGender(), is(Gender.MALE));
		assertThat(person2.getCreateDate(), is(notNullValue()));
		person.setGender(Gender.FEMALE);
		entityManager.update(person);

		assertThat(entityManager.existsNaturalId("test"), is(true));
		person2 = entityManager.findByNaturalId("test");
		assertThat(person2.getGender(), is(Gender.FEMALE));
		assertThat(entityManager.existsNaturalId("name", "test"), is(true));
		person2 = entityManager.findByNaturalId("name", "test");
		assertThat(person2.getGender(), is(Gender.FEMALE));

		try {
			entityManager.existsNaturalId("code", "9527");
		} catch (Exception e) {
			assertThat(e instanceof IllegalArgumentException, is(true));
		}

		try {
			entityManager.findByNaturalId("code", "9527");
		} catch (Exception e) {
			assertThat(e instanceof IllegalArgumentException, is(true));
		}

		person2 = entityManager.findOne("test");
		assertThat(person2.getGender(), is(Gender.FEMALE));
		person2 = entityManager.findOne(true, new Serializable[] { "name", "test" });
		assertThat(person2.getGender(), is(Gender.FEMALE));
		assertThat(entityManager.existsOne("name", "test"), is(true));
		assertThat(entityManager.existsOne(true, new Serializable[] { "name", "Test" }), is(true));
		assertThat(entityManager.existsOne(true, new Serializable[] { "name", null }), is(false));
		person2 = entityManager.findOne("name", "test");
		assertThat(person2.getGender(), is(Gender.FEMALE));

		person2 = entityManager.findOne(true, new Serializable[] { "code", "9527" });
		assertThat(person2.getGender(), is(Gender.FEMALE));
		assertThat(entityManager.existsOne("code", "9527"), is(true));
		assertThat(entityManager.existsOne(true, new Serializable[] { "code", "9527" }), is(true));
		person2 = entityManager.findOne("code", "9527");
		assertThat(person2.getGender(), is(Gender.FEMALE));

		assertThat(entityManager.exists(person.getId()), is(true));
		entityManager.delete(person2);
		assertThat(entityManager.exists(person.getId()), is(false));
		assertThat(entityManager.findByNaturalId("test"), is(nullValue()));

	}

	@Test
	public void testSaveWithAssignedId() {
		catManager.setEntityClass(Cat.class);
		Cat cat = new Cat("tom", "Tom");
		catManager.save(cat);
		assertThat(catManager.countAll(), is(1L));
		Cat cat2 = new Cat("tom", "Tom2");
		catManager.save(cat2);
		assertThat(catManager.countAll(), is(1L));
		assertThat(catManager.get("tom").getName(), is("Tom2"));
	}

	@Test(expected = ObjectOptimisticLockingFailureException.class)
	public void testVersion() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("test");
		person.setCode("9527");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		assertThat(person.getVersion(), is(-1));
		entityManager.save(person);
		assertThat(person.getVersion(), is(0));
		person.setCreateDate(new Date());
		entityManager.save(person);
		assertThat(person.getVersion(), is(1));
		entityManager.save(person);
		assertThat(person.getVersion(), is(2));
		entityManager.execute(session -> {
			Person p = session.get(Person.class, person.getId());
			assertThat(p.getVersion(), is(2));
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
		assertThat(person.lifyCycleEvents.size(), is(2));
		assertThat(person.lifyCycleEvents.get(0), is(PrePersist.class.getSimpleName()));
		assertThat(person.lifyCycleEvents.get(1), is(PostPersist.class.getSimpleName()));
		Person person2 = entityManager.get(person.getId());
		assertThat(person2.lifyCycleEvents.size(), is(1));
		assertThat(person2.lifyCycleEvents.get(0), is(PostLoad.class.getSimpleName()));
		person2.setDateOfBirth(new Date());
		entityManager.save(person2);
		assertThat(person2.lifyCycleEvents.size(), is(3));
		assertThat(person2.lifyCycleEvents.get(1), is(PreUpdate.class.getSimpleName()));
		assertThat(person2.lifyCycleEvents.get(2), is(PostUpdate.class.getSimpleName()));
		entityManager.delete(person2);
		assertThat(person2.lifyCycleEvents.size(), is(5));
		assertThat(person2.lifyCycleEvents.get(3), is(PreRemove.class.getSimpleName()));
		assertThat(person2.lifyCycleEvents.get(4), is(PostRemove.class.getSimpleName()));
	}

	@Test
	public void testCriteria() {
		entityManager.setEntityClass(Person.class);
		Person person = new Person();
		person.setName("test");
		person.setGender(Gender.MALE);
		person.setDateOfBirth(new Date());
		entityManager.save(person);
		assertThat(entityManager.countAll(), is(1L));
		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		assertThat(entityManager.countByCriteria(dc), is(1L));
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("name", "test"));
		assertThat(entityManager.existsCriteria(dc), is(true));
		Person person2 = entityManager.findByCriteria(dc);
		assertThat(person2.getName(), is("test"));
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.add(Restrictions.eq("name", "test"));
		assertThat(entityManager.existsCriteria(dc), is(true));
		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.FEMALE));
		assertThat(entityManager.existsCriteria(dc), is(false));
		entityManager.delete(person);
	}

	@Test
	public void testCriteriaWithList() {
		prepareData();

		List<Person> males = entityManager.findAll(Order.asc("name"));
		assertThat(males.size(), is(9));

		DetachedCriteria dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		males = entityManager.findListByCriteria(dc);
		assertThat(males.size(), is(5));

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		males = entityManager.findBetweenListByCriteria(dc, 2, 4);
		assertThat(males.size(), is(2));
		assertThat(males.get(0).getName(), is("test4"));
		assertThat(males.get(1).getName(), is("test6"));

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		males = entityManager.findListByCriteria(dc, 2, 3);
		assertThat(males.size(), is(2));
		assertThat(males.get(0).getName(), is("test6"));
		assertThat(males.get(1).getName(), is("test8"));

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		ResultPage<Person> rp = new ResultPage<>();
		rp.setPageNo(2);
		rp.setPageSize(3);
		rp.setCriteria(dc);
		rp = entityManager.findByResultPage(rp);
		males = (List<Person>) rp.getResult();
		assertThat(rp.getTotalPage(), is(2));
		assertThat(rp.getTotalResults(), is(5L));
		assertThat(males.size(), is(2));
		assertThat(males.get(0).getName(), is("test6"));
		assertThat(males.get(1).getName(), is("test8"));

		clearData();
		assertThat(entityManager.countAll(), is(0L));
	}

	@Test
	public void testHql() {
		prepareData();

		List<Person> males = entityManager.find("from Person p where p.gender=?1", Gender.MALE);
		assertThat(males.size(), is(5));
		entityManager.executeUpdate("delete from Person p where p.gender=?1", Gender.MALE);
		males = entityManager.find("from Person p where p.gender=?1", Gender.MALE);
		assertThat(males.size(), is(0));
		List<Person> females = entityManager.find("from Person p where p.gender=?1", Gender.FEMALE);
		assertThat(females.size(), is(4));

		clearData();
		assertThat(entityManager.countAll(), is(0L));

		prepareData();

		Map<String, Object> args = new HashMap<>();
		args.put("gender", Gender.MALE);
		males = entityManager.find("from Person p where p.gender=:gender", args);
		assertThat(males.size(), is(5));
		entityManager.executeUpdate("delete from Person p where p.gender=:gender", args);
		males = entityManager.find("from Person p where p.gender=:gender", args);
		assertThat(males.size(), is(0));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSql() {
		prepareData();
		List<Object[]> list = entityManager.executeFind(session -> {
			NativeQuery<Object[]> query = session.createSQLQuery("select * from Person where gender=:gender");
			query.setParameter("gender", 0);
			return query.getResultList();
		});
		assertThat(list.size(), is(5));
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
		assertThat(males.size(), is(5));
		assertThat(males.get(0).getName(), is("test0"));
		assertThat(males.get(0).getCode(), is("code0"));
		assertThat(males.get(0).getGender(), is(Gender.MALE));
		List<Map<String, Object>> males2 = entityManager.executeFind(session -> {
			return session.createQuery(hql).setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
					.setParameter("gender", Gender.MALE).list();
		});
		assertThat(males2.size(), is(5));
		assertThat(males2.get(0).get("name"), is("test0"));
		assertThat(males2.get(0).get("code"), is("code0"));
		assertThat(males2.get(0).get("gender"), is(Gender.MALE));
		List<Object[]> males3 = entityManager.executeFind(session -> {
			return session.createQuery(hql).setParameter("gender", Gender.MALE).list();
		});
		assertThat(males3.size(), is(5));
		assertThat(males3.get(0)[0], is("test0"));
		assertThat(males3.get(0)[1], is("code0"));
		assertThat(males3.get(0)[2], is(Gender.MALE));
		List<PersonDTO> males4 = entityManager.executeFind(session -> {
			DetachedCriteria dc = DetachedCriteria.forClass(Person.class);
			dc.setProjection(Projections.projectionList().add(Projections.property("name"), "name")
					.add(Projections.property("code"), "code").add(Projections.property("gender"), "gender"))
					.setResultTransformer(Transformers.aliasToBean(PersonDTO.class));
			dc.add(Restrictions.eq("gender", Gender.MALE));
			return dc.getExecutableCriteria(session).list();
		});
		assertThat(males4.size(), is(5));
		assertThat(males4.get(0).getName(), is("test0"));
		assertThat(males4.get(0).getCode(), is("code0"));
		assertThat(males4.get(0).getGender(), is(Gender.MALE));
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
		assertThat(person.getName(), is("test0"));
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
		assertThat(count, is(5L));
		assertThat(ai.get(), is(5));

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
		assertThat(count, is(9L));

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
		assertThat(entityManager.countByCriteria(dc), is(2 * 2L));

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
		assertThat(count, is(9L));
		assertThat(males.size(), is(9));

		dc = entityManager.detachedCriteria();
		dc.add(Restrictions.eq("gender", Gender.MALE));
		dc.addOrder(Order.asc("name"));
		count = entityManager.iterate(2, (entities, session) -> {
		}, dc, true);
		assertThat(count, is(9L));

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
			person.setCreateYearMonth(YearMonth.now());
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
