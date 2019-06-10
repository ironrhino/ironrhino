package org.ironrhino.core.jdbc;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.common.model.Gender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JdbcConfiguration.class)
public class JdbcRepositoryTest {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private DogRepository dogRepository;

	@Before
	public void setup() {
		personRepository.createTable();
	}

	@After
	public void cleanup() {
		personRepository.dropTable();
	}

	@Test
	public void testCrud() throws Exception {
		Person p = new Person();
		p.setName("test");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
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
		assertThat(p2, is(p));
		List<Person> all = personRepository.list();
		assertThat(all.size(), is(1));
		assertThat(all.get(0), is(p));
		List<Person> females = personRepository.listByGender(Gender.FEMALE);
		assertThat(females.size(), is(1));
		assertThat(females.get(0), is(p));
		List<Person> result = personRepository.search("te");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(p));
		assertThat(personRepository.count(), is(1L));
		assertThat(personRepository.countByNamePrefix("te"), is(1));
		assertThat(personRepository.listNames().size(), is(1));
		assertThat(personRepository.listGenders().size(), is(1));
		assertThat(personRepository.listAges().size(), is(1));
		assertThat(personRepository.updateAmount("test", new BigDecimal("11.00"), new BigDecimal("120.00")), is(false));
		assertThat(personRepository.updateAmount("test", new BigDecimal("12.00"), new BigDecimal("120.00")), is(true));
		assertThat(personRepository.updateAmountAndReturnIntRows("test", new BigDecimal("120.00"),
				new BigDecimal("12.00")), is(1));
		assertThat(personRepository.updateAmountAndReturnLongRows("test", new BigDecimal("12.00"),
				new BigDecimal("120.00")), is(1L));
		Person p3 = personRepository.get("test");
		assertThat(p3.getAmount(), is(new BigDecimal("120.00")));
		int rows = personRepository.delete("test");
		assertThat(rows, is(1));
		all = personRepository.list();
		assertThat(all.isEmpty(), is(true));
	}

	@Test
	public void testDefaultMethod() throws Exception {
		Person p = new Person();
		p.setName("test");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p = personRepository.getAndChangeAge(p.getName(), 0);
		assertThat(p.getName(), is("test"));
		assertThat(p.getAge(), is(0));
		p = personRepository.getAndChangeGender(p.getName(), Gender.MALE);
		assertThat(p.getName(), is("test"));
		assertThat(p.getGender(), is(Gender.MALE));
	}

	@Test
	public void testInCondition() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		assertThat(personRepository.getByNames(new String[] { "test" }).size(), is(0));
		assertThat(personRepository.getByNames(new String[] { "test1" }).size(), is(1));
		assertThat(personRepository.getByNames(new String[] { "test1", "test2" }).size(), is(2));
		assertThat(personRepository.getByNames(new String[] { "test1", "test2", "test" }).size(), is(2));
		assertThat(personRepository.getByNames(new String[] { "test1", "test2", "test3" }).size(), is(3));
		assertThat(personRepository.getByGenders(EnumSet.of(Gender.FEMALE)).size(), is(1));
		assertThat(personRepository.getByGenders(EnumSet.of(Gender.MALE)).size(), is(2));
		assertThat(personRepository.getByGenders(EnumSet.of(Gender.FEMALE, Gender.MALE)).size(), is(3));
	}

	@Test
	public void testConditionalSql() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		assertThat(personRepository.searchByNameOrGender(null, null).size(), is(3));
		assertThat(personRepository.searchByNameOrGender("test1", null).size(), is(1));
		assertThat(personRepository.searchByNameOrGender("test1", Gender.FEMALE).size(), is(1));
		assertThat(personRepository.searchByNameOrGender("test1", Gender.MALE).size(), is(0));
		assertThat(personRepository.searchByNameOrGender(null, Gender.MALE).size(), is(2));
	}

	@Test
	public void testNestedProperty() throws Exception {
		Person p = new Person();
		p.setName("test");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		Person p2 = personRepository.getWithShadow(p.getName());
		assertThat(p2.getShadow(), is(notNullValue()));
		assertThat(p2.getShadow().getName(), is(p2.getName()));
		assertThat(p2.getShadow().getGender(), is(p2.getGender()));
		assertThat(p2.getShadow().getDob(), is(p2.getDob()));
		assertThat(p2.getShadow().getAge(), is(p2.getAge()));
		assertThat(p2.getShadow().getAmount(), is(p2.getAmount()));
	}

	@Test
	public void testLimiting() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		assertThat(personRepository.searchWithLimiting("test", Limiting.of(10)).size(), is(3));
		assertThat(personRepository.searchWithLimiting("test", Limiting.of(3)).size(), is(3));
		assertThat(personRepository.searchWithLimiting("test", Limiting.of(2)).size(), is(2));
		assertThat(personRepository.searchWithLimiting("test", Limiting.of(1)).size(), is(1));
		assertThat(personRepository.searchWithLimiting("test", Limiting.of(0)).size(), is(0));
		List<Person> list = personRepository.searchWithLimiting("test", Limiting.of(1, 2));
		assertThat(list.size(), is(2));
		assertThat(list.get(0).getName(), is("test2"));
		list = personRepository.searchWithLimiting("test", Limiting.of(2, 2));
		assertThat(list.size(), is(1));
		assertThat(list.get(0).getName(), is("test3"));
	}

	@Test
	public void testGeneratedKey() throws Exception {
		dogRepository.createTable();
		int size = 10;
		for (int i = 0; i < size; i++) {
			Dog dog = new Dog();
			dog.setName("dog" + i);
			dogRepository.save(dog);
			assertThat(dog.getId(), is(Integer.valueOf(i + 1)));
		}
		for (int i = 0; i < size; i++) {
			Dog dog = new Dog();
			dog.setName("dog" + i);
			dogRepository.insert(dog.getName(), id -> {
				dog.setId(id);
			});
			assertThat(dog.getId(), is(Integer.valueOf(size + i + 1)));
		}
		for (int i = 0; i < size * 2; i++) {
			assertThat(dogRepository.delete(i + 1), is(true));
		}
		dogRepository.dropTable();
	}

	@Test
	public void testOptional() throws Exception {
		Person p = new Person();
		p.setName("test");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
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
		Optional<Person> optional = personRepository.getOptional(p.getName());
		assertThat(optional.isPresent(), is(true));
		assertThat(optional.get(), is(p));
		assertThat(personRepository.getOptional("notexists").isPresent(), is(false));
	}

	@Test
	public void testRowCallbackHanlder() throws Exception {
		Person p = new Person();
		p.setName("test1");
		p.setDob(LocalDate.now());
		p.setSince(YearMonth.now());
		p.setAge(11);
		p.setGender(Gender.FEMALE);
		p.setAmount(new BigDecimal("12.00"));
		personRepository.save(p);
		p.setName("test2");
		p.setGender(Gender.MALE);
		personRepository.save(p);
		p.setName("test3");
		personRepository.save(p);
		AtomicInteger count = new AtomicInteger();
		personRepository.searchWithLimiting("test", Limiting.of(2), new RowCallbackHandler() {
			@Override
			public void processRow(ResultSet rs) throws SQLException {
				count.incrementAndGet();
			}
		});
		assertThat(count.get(), is(2));
	}

}
