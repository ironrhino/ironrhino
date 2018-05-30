package org.ironrhino.core.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.cache.CacheAspectTest.CacheConfiguration;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.Data;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CacheConfiguration.class)
public class CacheAspectTest {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private CacheManager cacheManager;

	@Test
	public void test() {
		Person person = new Person();
		person.setName("test");
		personRepository.save(person);
		for (int i = 0; i < 10; i++) {
			if (i == 0) {
				assertNull(cacheManager.get(person.getName(), PersonRepository.CACHE_NAMESPACE));
			} else {
				assertEquals(person, cacheManager.get(person.getName(), PersonRepository.CACHE_NAMESPACE));
			}
			Person person2 = personRepository.get(person.getName());
			assertEquals(person, person2);
			assertEquals(1, personRepository.count());
		}
		personRepository.remove(person.getName());
		assertNull(cacheManager.get(person.getName(), PersonRepository.CACHE_NAMESPACE));
		assertNull(personRepository.get(person.getName()));
	}

	@Data
	public static class Person implements Serializable {

		private static final long serialVersionUID = -4928303383834547829L;

		private String name;

	}

	public static class PersonRepository {

		public static final String CACHE_NAMESPACE = "person";

		public Map<String, Person> people = new ConcurrentHashMap<>();

		public AtomicInteger count = new AtomicInteger();

		@EvictCache(key = "${person.name}", namespace = CACHE_NAMESPACE)
		public void save(Person person) {
			people.put(person.getName(), person);
		}

		@CheckCache(key = "${name}", namespace = CACHE_NAMESPACE)
		public Person get(String name) {
			count.incrementAndGet();
			return people.get(name);
		}

		@EvictCache(key = "${name}", namespace = CACHE_NAMESPACE)
		public void remove(String name) {
			people.remove(name);
		}

		protected int count() {
			return count.get();
		}

	}

	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class CacheConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new Cache2kCacheManager();
		}

		@Bean
		public CacheAspect cacheAspect() {
			return new CacheAspect();
		}

		@Bean
		public PersonRepository personRepository() {
			return new PersonRepository();
		}

	}

}
