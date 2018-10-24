package org.ironrhino.core.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.ConstraintViolationException;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.sample.remoting.PersonRepository;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestService.FutureType;
import org.ironrhino.security.domain.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.concurrent.ListenableFuture;

public abstract class RemotingServiceTestsBase {

	public static final int THREADS = 100;

	public static final int LOOP = 100;

	private static ExecutorService executorService;

	@Autowired
	protected TestService testService;

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
		assertNotNull(personRepository.findAll());
	}

	@Test
	public void testEcho() {
		testService.ping();
		assertEquals("test", testService.defaultEcho("test"));
		assertEquals("", testService.echo());
		assertNull(testService.echo((String) null));
		assertEquals("test", testService.echo("test"));
		assertEquals(Collections.singletonList("list"), testService.echoList(Collections.singletonList("list")));
		assertTrue(Arrays.equals(new String[] { "echoWithArrayList" },
				testService.echoArray(new String[] { "echoWithArrayList" })));
		assertEquals(3, testService.countAndAdd(Collections.singletonList("test"), 2));
		assertNull(testService.loadUserByUsername(null));
		assertEquals("username", testService.loadUserByUsername("username").getUsername());
		assertNull(testService.search(null));
		assertEquals(Collections.EMPTY_LIST, testService.search(""));
		assertEquals("username", testService.search("username").get(0).getUsername());
	}

	@Test
	public void testEchoListWithArray() {
		assertEquals("test",
				testService.echoListWithArray(Collections.singletonList(new String[] { "test" })).get(0)[0]);
	}

	@Test
	public void testUserDetails() {
		assertNull(testService.loadUserByUsername(null));
		assertEquals("username", testService.loadUserByUsername("username").getUsername());
		assertNull(testService.search(null));
		assertEquals(Collections.EMPTY_LIST, testService.search(""));
		assertEquals("username", testService.search("username").get(0).getUsername());
	}

	@Test(expected = ConstraintViolationException.class)
	public void testBeanValidation() throws Exception {
		assertEquals(Scope.LOCAL, testService.echo(Scope.LOCAL));
		testService.echo((Scope) null);
	}

	@Test(expected = ConstraintViolationException.class)
	public void testBeanValidationWithValid() throws Exception {
		User user = new User();
		user.setEmail("test@test.com");
		assertEquals(user, testService.echo(user));
		user.setEmail("iamnotemail");
		testService.echo(user);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOnException() throws Exception {
		testService.throwException("this is a message");
	}

	@Test
	public void testOptional() {
		assertFalse(testService.loadOptionalUserByUsername("").isPresent());
		assertEquals("username", testService.loadOptionalUserByUsername("username").get().getUsername());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOptionalWithException() {
		testService.loadOptionalUserByUsername(null);
	}

	@Test
	public void testFuture() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			assertEquals("username", testService.loadFutureUserByUsername("username", futureType).get().getUsername());
		}
	}

	@Test
	public void testFutureWithNullUsername() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			boolean error = false;
			try {
				testService.loadFutureUserByUsername(null, futureType).get();
			} catch (ExecutionException e) {
				assertTrue(e.getCause() instanceof IllegalArgumentException);
				error = true;
			}
			assertTrue(error);
		}
	}

	@Test
	public void testFutureWithBlankUsername() throws Exception {
		for (FutureType futureType : FutureType.values()) {
			boolean error = false;
			try {
				testService.loadFutureUserByUsername("", futureType).get();
			} catch (ExecutionException e) {
				assertTrue(e.getCause() instanceof IllegalArgumentException);
				error = true;
			}
			assertTrue(error);
		}
	}

	@Test
	public void testListenableFuture() throws Exception {
		ListenableFuture<UserDetails> future = testService.loadListenableFutureUserByUsername("username");
		AtomicBoolean b1 = new AtomicBoolean();
		AtomicBoolean b2 = new AtomicBoolean();
		future.addCallback(u -> {
			b1.set("username".equals(u.getUsername()));
		}, e -> {
			b2.set(true);
		});
		Thread.sleep(1000);
		assertTrue(b1.get());
		assertFalse(b2.get());
		assertEquals("username", future.get().getUsername());
	}

	@Test
	public void testCallable() throws Exception {
		assertEquals("username", testService.loadCallableUserByUsername("username").call().getUsername());
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
	public void testConcurreny() throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(THREADS);
		final AtomicInteger count = new AtomicInteger();
		long time = System.currentTimeMillis();
		for (int i = 0; i < THREADS; i++) {

			executorService.execute(() -> {
				for (int j = 0; j < LOOP; j++) {
					assertEquals("test" + j, testService.echo("test" + j));
					count.incrementAndGet();
				}
				cdl.countDown();
			});
		}
		cdl.await();
		time = System.currentTimeMillis() - time;
		System.out.println(getClass().getSimpleName() + " completed " + count.get() + " requests with concurrency("
				+ THREADS + ") in " + time + "ms (tps = " + (count.get() * 1000 / time) + ")");
		assertEquals(count.get(), THREADS * LOOP);
	}

}
