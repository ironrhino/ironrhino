package org.ironrhino.core.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.sample.remoting.PersonRepository;
import org.ironrhino.sample.remoting.TestService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class RemoteServiceTestsBase {

	public static final int THREADS = 100;

	public static final int LOOP = 100;

	private static ExecutorService executorService;

	@Autowired
	private TestService testService;

	@Autowired
	private PersonRepository personRepository;

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
		assertEquals("", testService.echo());
		assertNull(testService.echo(null));
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
	public void testUserDetails() {
		assertNull(testService.loadUserByUsername(null));
		assertEquals("username", testService.loadUserByUsername("username").getUsername());
		assertNull(testService.search(null));
		assertEquals(Collections.EMPTY_LIST, testService.search(""));
		assertEquals("username", testService.search("username").get(0).getUsername());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOnException() throws Exception {
		testService.throwException("this is a message");
	}

	@Test
	public void testConcurreny() throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(THREADS);
		final AtomicInteger count = new AtomicInteger();
		long time = System.currentTimeMillis();
		for (int i = 0; i < THREADS; i++) {

			executorService.execute(new Runnable() {
				@Override
				public void run() {
					for (int j = 0; j < LOOP; j++) {
						assertEquals("test" + j, testService.echo("test" + j));
						count.incrementAndGet();
					}
					cdl.countDown();
				}
			});
		}
		cdl.await();
		time = System.currentTimeMillis() - time;
		System.out.println("completed " + count.get() + " requests with concurrency(" + THREADS + ") in " + time
				+ "ms (tps = " + (count.get() * 1000 / time) + ")");
		assertEquals(count.get(), THREADS * LOOP);
	}

}
