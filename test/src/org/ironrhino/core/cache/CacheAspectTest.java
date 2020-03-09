package org.ironrhino.core.cache;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.ironrhino.core.cache.CacheAspectTest.AnotherPersonRepository;
import org.ironrhino.core.cache.CacheAspectTest.CacheConfiguration;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CacheConfiguration.class)
@TestPropertySource(properties = AnotherPersonRepository.KEY_CACHE_NAMESPACE + "="
		+ CacheAspectTest.CUSTOMIZED_CACHE_NAMESPACE)
public class CacheAspectTest {

	public static final String CUSTOMIZED_CACHE_NAMESPACE = "anotherPerson";

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private AnotherPersonRepository anotherPersonRepository;

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
				assertThat(cacheManager.get(person.getName(), PersonRepository.CACHE_NAMESPACE), is(nullValue()));
			} else {
				assertThat(cacheManager.get(person.getName(), PersonRepository.CACHE_NAMESPACE), is(person));
			}
			Person person2 = personRepository.get(person.getName());
			assertThat(person2, is(person));
			assertThat(personRepository.count(), is(1));
		}
		personRepository.remove(person.getName());
		assertThat(cacheManager.get(person.getName(), PersonRepository.CACHE_NAMESPACE), is(nullValue()));
	}

	@Test
	public void testCacheNamespaceProvider() {
		Person person = new Person();
		person.setName("test");
		anotherPersonRepository.save(person);
		for (int i = 0; i < 10; i++) {
			if (i == 0) {
				assertThat(cacheManager.get(person.getName(), CacheAspectTest.CUSTOMIZED_CACHE_NAMESPACE),
						is(nullValue()));
			} else {
				assertThat(cacheManager.get(person.getName(), CacheAspectTest.CUSTOMIZED_CACHE_NAMESPACE), is(person));
			}
			Person person2 = anotherPersonRepository.get(person.getName());
			assertThat(person2, is(person));
			assertThat(anotherPersonRepository.count(), is(1));
		}
		anotherPersonRepository.remove(person.getName());
		assertThat(cacheManager.get(person.getName(), CacheAspectTest.CUSTOMIZED_CACHE_NAMESPACE), is(nullValue()));
	}

	@Test
	public void testCacheNull() {
		for (int i = 0; i < 10; i++) {
			assertThat(personRepository.get("notexists"), is(nullValue()));
			assertThat(personRepository.count(), is(i + 1));
		}
		int current = personRepository.count();
		for (int i = 0; i < 10; i++) {
			assertThat(personRepository.getWithCacheNull("notexists"), is(nullValue()));
			assertThat(personRepository.count(), is(current + 1));
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
		assertThat(personRepository.count() <= PersonRepository.THROUGH_PERMITS, is(true));
		es.shutdown();
	}

	@Test
	public void testJdkDynamicProxy() throws Exception {
		long nanoTime = timeService.nanoTime();
		for (int i = 0; i < 5; i++) {
			Thread.sleep(10);
			assertThat(timeService.nanoTime(), is(nanoTime));
		}
		cacheManager.delete(TimeService.CACHE_KEY, TimeService.CACHE_NAMESPACE);
		assertThat(timeService.nanoTime(), is(not(nanoTime)));
		nanoTime = timeService.nanoTime();
		for (int i = 0; i < 5; i++) {
			Thread.sleep(10);
			assertThat(timeService.nanoTime(), is(nanoTime));
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

	public static class AnotherPersonRepository implements CacheNamespaceProvider {

		public static final String KEY_CACHE_NAMESPACE = "anotherPersonRepository.cacheNamespace";
		public static final String DEFAULT_CACHE_NAMESPACE = "person";
		public static final int THROUGH_PERMITS = 10;

		@Getter
		@Setter
		@Value("${" + KEY_CACHE_NAMESPACE + ":" + DEFAULT_CACHE_NAMESPACE + "}")
		private String cacheNamespace;

		public Map<String, Person> people = new ConcurrentHashMap<>();

		public AtomicInteger count = new AtomicInteger();

		@EvictCache(key = "${person.name}")
		public void save(Person person) {
			people.put(person.getName(), person);
		}

		@CheckCache(key = "${name}", throughPermits = THROUGH_PERMITS)
		public Person get(String name) {
			count.incrementAndGet();
			return people.get(name);
		}

		@CheckCache(key = "${name}", cacheNull = true)
		public Person getWithCacheNull(String name) {
			count.incrementAndGet();
			return people.get(name);
		}

		@EvictCache(key = "${name}")
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

	public interface TimeService {

		String CACHE_NAMESPACE = "time";
		String CACHE_KEY = "nano";

		@CheckCache(key = CACHE_KEY, namespace = CACHE_NAMESPACE)
		long nanoTime();

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
		public AnotherPersonRepository anotherPersonRepository() {
			return new AnotherPersonRepository();
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
