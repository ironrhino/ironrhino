package org.ironrhino.core.remoting;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.ConstraintViolationException;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.sample.remoting.BarService;
import org.ironrhino.sample.remoting.FooService;
import org.ironrhino.sample.remoting.PersonRepository;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestService.FutureType;
import org.ironrhino.security.domain.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JavaRemotingServiceTests.Config.class)
@TestPropertySource(properties = "httpInvoker.serializationType=JAVA")
public class JavaRemotingServiceTests {

	public static final int THREADS = 100;

	public static final int LOOP = 100;

	private static ExecutorService executorService;

	@Autowired
	protected TestService testService;

	@Autowired
	protected FooService fooService;

	@Autowired
	protected BarService barService;

	@Autowired
	protected PersonRepository personRepository;

	@BeforeClass
	public static void setup() {
		executorService = Executors.newFixedThreadPool(THREADS);
	}

	@AfterClass
	public static void destroy() {
		executorService.shutdown();
	}

	@Test
	public void testJdbcRepository() {
		assertThat(personRepository.findAll(), is(notNullValue()));
	}

	@Test
	public void testServiceImplementedByFactoryBean() {
		assertThat(fooService.test("test"), is("test"));
	}

	@Test
	public void testServiceRegistriedInConfigurationClass() {
		assertThat(barService.test("test"), is("test"));
	}

	@Test
	public void testEcho() {
		testService.ping();
		assertThat(testService.defaultEcho("test"), is("test"));
		assertThat(testService.echo(), is(""));
		assertThat(testService.echo((String) null), is(nullValue()));
		assertThat(testService.echo("test"), is("test"));
		assertThat(testService.echoList(Collections.singletonList("list")), is(Collections.singletonList("list")));
		assertThat(Arrays.equals(new String[] { "echoWithArrayList" },
				testService.echoArray(new String[] { "echoWithArrayList" })), is(true));
		assertThat(testService.countAndAdd(Collections.singletonList("test"), 2), is(3));
		TestService.Immutable value = new TestService.Immutable(12, "test");
		assertThat(testService.echoImmutable(value), is(value));
	}

	@Test
	public void testEchoListWithArray() {
		assertThat(testService.echoListWithArray(Collections.singletonList(new String[] { "test" })).get(0)[0],
				is("test"));
	}

	@Test
	public void testConcreteType() {
		assertThat(testService.loadUserByUsername(null), is(nullValue()));
		assertThat(testService.loadUserByUsername("username").getUsername(), is("username"));
		assertThat(testService.searchUser(null), is(nullValue()));
		assertThat(testService.searchUser(""), is(Collections.EMPTY_LIST));
		assertThat(testService.searchUser("username").get(0).getUsername(), is("username"));
	}

	@Test
	public void testNonConcreteType() {
		User user = new User();
		user.setUsername("test");
		assertThat(testService.echoUserDetails(user).getUsername(), is(user.getUsername()));
		assertThat(testService.loadUserDetailsByUsername(null), is(nullValue()));
		assertThat(testService.loadUserDetailsByUsername("username").getUsername(), is("username"));
		assertThat(testService.searchUserDetails(null), is(nullValue()));
		assertThat(testService.searchUserDetails(""), is(Collections.EMPTY_LIST));
		assertThat(testService.searchUserDetails("username").get(0).getUsername(), is("username"));
	}

	@Test(expected = ConstraintViolationException.class)
	public void testBeanValidation() throws Exception {
		assertThat(testService.echoScope(Scope.LOCAL), is(Scope.LOCAL));
		testService.echoScope((Scope) null);
	}

	@Test(expected = ConstraintViolationException.class)
	public void testBeanValidationWithValid() throws Exception {
		User user = new User();
		user.setEmail("test@test.com");
		assertThat(testService.echoUser(user), is(user));
		user.setEmail("iamnotemail");
		testService.echoUser(user);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOnException() throws Exception {
		testService.throwException("this is a message");
	}

	@Test
	public void testConcreteOptional() {
		assertThat(testService.loadOptionalUserByUsername("").isPresent(), is(false));
		assertThat(testService.loadOptionalUserByUsername("username").get().getUsername(), is("username"));
	}

	@Test
	public void testNonConcreteOptional() {
		assertThat(testService.loadOptionalUserDetailsByUsername("").isPresent(), is(false));
		assertThat(testService.loadOptionalUserDetailsByUsername("username").get().getUsername(), is("username"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConcreteOptionalWithException() {
		testService.loadOptionalUserByUsername(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonConcreteOptionalWithException() {
		testService.loadOptionalUserDetailsByUsername(null);
	}

	@Test
	public void testConcreteFuture() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			assertThat(testService.loadFutureUserByUsername("username", futureType).get().getUsername(),
					is("username"));
		}
	}

	@Test
	public void testNonConcreteFuture() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			assertThat(testService.loadFutureUserDetailsByUsername("username", futureType).get().getUsername(),
					is("username"));
		}
	}

	@Test
	public void testFutureWithNullUsername() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			boolean error = false;
			try {
				testService.loadFutureUserByUsername(null, futureType).get();
			} catch (ExecutionException e) {
				assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
				error = true;
			}
			assertThat(error, is(true));
		}
	}

	@Test
	public void testFutureWithBlankUsername() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			boolean error = false;
			try {
				testService.loadFutureUserByUsername("", futureType).get();
			} catch (ExecutionException e) {
				assertThat(e.getCause() instanceof IllegalArgumentException, is(true));
				error = true;
			}
			assertThat(error, is(true));
		}
	}

	@Test
	public void testConcreteListenableFuture() throws Exception {
		ListenableFuture<User> future = testService.loadListenableFutureUserByUsername("username");
		AtomicBoolean b1 = new AtomicBoolean();
		AtomicBoolean b2 = new AtomicBoolean();
		future.addCallback(u -> {
			b1.set("username".equals(u.getUsername()));
		}, e -> {
			b2.set(true);
		});
		Thread.sleep(1000);
		assertThat(b1.get(), is(true));
		assertThat(b2.get(), is(false));
		assertThat(future.get().getUsername(), is("username"));
	}

	@Test
	public void testNonConcreteListenableFuture() throws Exception {
		ListenableFuture<? extends UserDetails> future = testService
				.loadListenableFutureUserDetailsByUsername("username");
		AtomicBoolean b1 = new AtomicBoolean();
		AtomicBoolean b2 = new AtomicBoolean();
		future.addCallback(u -> {
			b1.set("username".equals(u.getUsername()));
		}, e -> {
			b2.set(true);
		});
		Thread.sleep(1000);
		assertThat(b1.get(), is(true));
		assertThat(b2.get(), is(false));
		assertThat(future.get().getUsername(), is("username"));
	}

	@Test
	public void testConcreteCompletableFuture() throws Exception {
		CompletableFuture<User> future = testService.loadCompletableFutureUserByUsername("username");
		assertThat(future.get().getUsername(), is("username"));
	}

	@Test
	public void testNonConcreteCompletableFuture() throws Exception {
		CompletableFuture<? extends UserDetails> future = testService
				.loadCompletableFutureUserDetailsByUsername("username");
		assertThat(future.get().getUsername(), is("username"));
	}

	@Test
	public void testNonConcreteCompletionStage() throws Exception {
		testService.loadCompletionStageUserDetailsByUsername("username").thenAccept(ud -> {
			assertThat(ud.getUsername(), is("username"));
		});
		Thread.sleep(500);
	}

	@Test
	public void testConcreteCallable() throws Exception {
		assertThat(testService.loadCallableUserByUsername("username").call().getUsername(), is("username"));
	}

	@Test
	public void testNonConcreteCallable() throws Exception {
		assertThat(testService.loadCallableUserDetailsByUsername("username").call().getUsername(), is("username"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCallableWithNullUsername() throws Exception {
		testService.loadCallableUserByUsername(null).call();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCallableWithBlankUsername() throws Exception {
		testService.loadCallableUserByUsername("").call();
	}

	@Test
	public void testGeneric() throws Exception {
		User user = new User();
		user.setUsername("username");
		assertThat(testService.echoGenericUserDetails(user).getUsername(), is(user.getUsername()));
	}

	@Test
	public void testConcurreny() throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(THREADS);
		final AtomicInteger count = new AtomicInteger();
		long time = System.currentTimeMillis();
		for (int i = 0; i < THREADS; i++) {

			executorService.execute(() -> {
				for (int j = 0; j < LOOP; j++) {
					assertThat(testService.echo("test" + j), is("test" + j));
					count.incrementAndGet();
				}
				cdl.countDown();
			});
		}
		cdl.await();
		time = System.currentTimeMillis() - time;
		System.out.println(getClass().getSimpleName() + " completed " + count.get() + " requests with concurrency("
				+ THREADS + ") in " + time + "ms (tps = " + (count.get() * 1000 / time) + ")");
		assertThat(THREADS * LOOP, is(count.get()));
	}

	@Configuration
	static class Config {

		@Bean
		public HttpInvokerClient testService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(TestService.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

		@Bean
		public HttpInvokerClient fooService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(FooService.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

		@Bean
		public HttpInvokerClient barService() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(BarService.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

		@Bean
		public HttpInvokerClient personRepository() {
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(PersonRepository.class);
			hic.setBaseUrl("http://localhost:8080");
			return hic;
		}

		@Bean
		public LocalValidatorFactoryBean validatorFactory() {
			return new LocalValidatorFactoryBean();
		}

	}

}