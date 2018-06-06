package org.ironrhino.core.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.ironrhino.core.cache.CacheAspectTest.CacheConfiguration;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
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
	private TimeService timeService;

	@Autowired
	private CacheManager cacheManager;

	@Before
	public void init() {
		personRepository.clearCount();
	}

	@After
	public void destroy() {
		cacheManager.delete("test", PersonRepository.CACHE_NAMESPACE);
		cacheManager.delete("notexists", PersonRepository.CACHE_NAMESPACE);
		cacheManager.delete("nano", TimeService.CACHE_NAMESPACE);
	}

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
	}

	@Test
	public void testCacheNull() {
		for (int i = 0; i < 10; i++) {
			assertNull(personRepository.get("notexists"));
			assertEquals(i + 1, personRepository.count());
		}
		int current = personRepository.count();
		for (int i = 0; i < 10; i++) {
			assertNull(personRepository.getWithCacheNull("notexists"));
			assertEquals(current + 1, personRepository.count());
		}
	}

	@Test
	public void testConcurrency() throws Exception {
		Person person = new Person();
		person.setName("test");
		personRepository.save(person);
		int THREADS = PersonRepository.THROUGH_PERMITS * 10;
		ExecutorService es = Executors.newFixedThreadPool(THREADS);
		CountDownLatch cdl = new CountDownLatch(THREADS);
		for (int n = 0; n < THREADS; n++) {
			es.execute(() -> {
				for (int i = 0; i < 10; i++) {
					personRepository.get(person.getName());
				}
				cdl.countDown();
			});
		}
		cdl.await();
		assertTrue(personRepository.count() <= PersonRepository.THROUGH_PERMITS);
		es.shutdown();
	}

	@Test
	public void testJdkDynamicProxy() throws Exception {
		long nanoTime = timeService.nanoTime();
		for (int i = 0; i < 5; i++) {
			Thread.sleep(10);
			assertEquals(nanoTime, timeService.nanoTime());
		}
		cacheManager.delete(TimeService.CACHE_KEY, TimeService.CACHE_NAMESPACE);
		assertNotEquals(nanoTime, timeService.nanoTime());
		nanoTime = timeService.nanoTime();
		for (int i = 0; i < 5; i++) {
			Thread.sleep(10);
			assertEquals(nanoTime, timeService.nanoTime());
		}
	}

	@Data
	public static class Person implements Serializable {

		private static final long serialVersionUID = -4928303383834547829L;

		private String name;

	}

	public static class PersonRepository {

		public static final String CACHE_NAMESPACE = "person";
		public static final int THROUGH_PERMITS = 10;

		public Map<String, Person> people = new ConcurrentHashMap<>();

		public AtomicInteger count = new AtomicInteger();

		@EvictCache(key = "${person.name}", namespace = CACHE_NAMESPACE)
		public void save(Person person) {
			people.put(person.getName(), person);
		}

		@CheckCache(key = "${name}", throughPermits = THROUGH_PERMITS, namespace = CACHE_NAMESPACE)
		public Person get(String name) {
			count.incrementAndGet();
			return people.get(name);
		}

		@CheckCache(key = "${name}", cacheNull = true, namespace = CACHE_NAMESPACE)
		public Person getWithCacheNull(String name) {
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

		protected void clearCount() {
			count.set(0);
		}

	}

	public static interface TimeService {

		public static final String CACHE_NAMESPACE = "time";
		public static final String CACHE_KEY = "nano";

		@CheckCache(key = CACHE_KEY, namespace = CACHE_NAMESPACE)
		public long nanoTime();

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

		@Bean
		public FactoryBean<TimeService> accountService() {
			return new FactoryBean<TimeService>() {

				@Override
				public TimeService getObject() throws Exception {
					return (TimeService) new ProxyFactory(TimeService.class, (MethodInterceptor) (mi -> {
						return System.nanoTime();
					})).getProxy(TimeService.class.getClassLoader());
				}

				@Override
				public Class<?> getObjectType() {
					return TimeService.class;
				}

			};
		}

	}

}
