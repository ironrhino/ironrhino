package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.ironrhino.common.model.Gender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(locations = { "ctx.xml" })
public class JdbcRepositoryTest {

	@Autowired
	private PersonRepository personRepository;

	@Before
	public void setup() {
		personRepository.createTable();
	}

	@After
	public void cleanup() {
		personRepository.dropTable();
	}

	@Test
	public void test() throws Exception {
		Person p = new Person();
		p.setName("test");
		p.setDob(LocalDate.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		p.setAttributes(new HashMap<>());
		p.getAttributes().put("key1", "value1");
		p.getAttributes().put("key2", "value2");
		p.setRoles(new LinkedHashSet<>());
		p.getRoles().add("test1");
		p.getRoles().add("test2");
		personRepository.save(p);
		Person p2 = personRepository.get(p.getName());
		assertEquals(p, p2);
		List<Person> all = personRepository.list();
		assertEquals(1, all.size());
		assertEquals(p, all.get(0));
		List<Person> females = personRepository.listByGender(Gender.FEMALE);
		assertEquals(1, females.size());
		assertEquals(p, females.get(0));
		List<Person> result = personRepository.search("te");
		assertEquals(1, result.size());
		assertEquals(p, result.get(0));
		assertEquals(1, personRepository.count());
		assertEquals(1, personRepository.countByNamePrefix("te"));
		assertEquals(1, personRepository.listNames().size());
		assertEquals(1, personRepository.listGenders().size());
		assertEquals(1, personRepository.listAges().size());
		int rows = personRepository.delete("test");
		assertEquals(1, rows);
		all = personRepository.list();
		assertTrue(all.isEmpty());
	}

	@Test
	public void testInCondition() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		assertEquals(0, personRepository.getByNames(new String[] { "test" }).size());
		assertEquals(1, personRepository.getByNames(new String[] { "test1" }).size());
		assertEquals(2, personRepository.getByNames(new String[] { "test1", "test2" }).size());
		assertEquals(2, personRepository.getByNames(new String[] { "test1", "test2", "test" }).size());
		assertEquals(3, personRepository.getByNames(new String[] { "test1", "test2", "test3" }).size());
		assertEquals(1, personRepository.getByGenders(EnumSet.of(Gender.FEMALE)).size());
		assertEquals(2, personRepository.getByGenders(EnumSet.of(Gender.MALE)).size());
		assertEquals(3, personRepository.getByGenders(EnumSet.of(Gender.FEMALE, Gender.MALE)).size());
	}

	@Test
	public void testConditionalSql() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		assertEquals(3, personRepository.searchByNameOrGender(null, null).size());
		assertEquals(1, personRepository.searchByNameOrGender("test1", null).size());
		assertEquals(1, personRepository.searchByNameOrGender("test1", Gender.FEMALE).size());
		assertEquals(0, personRepository.searchByNameOrGender("test1", Gender.MALE).size());
		assertEquals(2, personRepository.searchByNameOrGender(null, Gender.MALE).size());
	}

	@Test
	public void testNestedProperty() throws Exception {
		Person p = new Person();
		p.setName("test");
		p.setDob(LocalDate.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		Person p2 = personRepository.getWithShadow(p.getName());
		assertNotNull(p2.getShadow());
		assertEquals(p2.getName(), p2.getShadow().getName());
		assertEquals(p2.getGender(), p2.getShadow().getGender());
		assertEquals(p2.getDob(), p2.getShadow().getDob());
		assertEquals(p2.getAge(), p2.getShadow().getAge());
		assertEquals(p2.getAmount(), p2.getShadow().getAmount());
	}

	@Test
	public void testLimiting() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		assertEquals(3, personRepository.searchWithLimiting("test", Limiting.of(10)).size());
		assertEquals(3, personRepository.searchWithLimiting("test", Limiting.of(3)).size());
		assertEquals(2, personRepository.searchWithLimiting("test", Limiting.of(2)).size());
		assertEquals(1, personRepository.searchWithLimiting("test", Limiting.of(1)).size());
		assertEquals(0, personRepository.searchWithLimiting("test", Limiting.of(0)).size());
		List<Person> list = personRepository.searchWithLimiting("test", Limiting.of(1, 2));
		assertEquals(2, list.size());
		assertEquals("test2", list.get(0).getName());
		list = personRepository.searchWithLimiting("test", Limiting.of(2, 2));
		assertEquals(1, list.size());
		assertEquals("test3", list.get(0).getName());
	}

}
