package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import org.ironrhino.common.model.Gender;
import org.ironrhino.core.util.DateUtils;
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

	@Test
	public void test() throws Exception {
		personRepository.createTable();
		Person p = new Person();
		p.setName("test");
		p.setDob(DateUtils.parseDate10("2000-12-12"));
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal(12));
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
		assertEquals(1, personRepository.listAges().size());
		int rows = personRepository.delete("test");
		assertEquals(1, rows);
		all = personRepository.list();
		assertTrue(all.isEmpty());
		personRepository.dropTable();
	}

	@Test
	public void testInCondition() throws Exception {
		personRepository.createTable();
		Person p = new Person();
		p.setName("test1");
		p.setDob(DateUtils.parseDate10("2000-12-12"));
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal(12));
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

}
