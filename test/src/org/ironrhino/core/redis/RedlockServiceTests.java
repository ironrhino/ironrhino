package org.ironrhino.core.redis;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.redis.RedlockServiceTests.RedlockServiceConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedlockServiceConfiguration.class)
@TestPropertySource(properties = "redlock.addresses=localhost")
public class RedlockServiceTests {

	public static final int THREADS = 10;

	public static final int LOOP = 100;

	private static ExecutorService executorService;

	@Autowired
	private Redlock redlock;

	@BeforeClass
	public static void setup() {
		executorService = Executors.newFixedThreadPool(THREADS);
	}

	@AfterClass
	public static void destroy() {
		executorService.shutdown();
	}

	@Test
	public void testTryLock() throws InterruptedException {
		final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
		final CountDownLatch cdl = new CountDownLatch(THREADS);
		final AtomicInteger success = new AtomicInteger();
		final AtomicInteger failed = new AtomicInteger();
		final AtomicInteger error = new AtomicInteger();
		long time = System.currentTimeMillis();
		for (int i = 0; i < THREADS; i++) {

			executorService.execute(() -> {
				for (int j = 0; j < LOOP; j++) {
					try {
						String lockName = "lock" + System.currentTimeMillis() % 10;
						if (redlock.tryLock(lockName, 10, TimeUnit.SECONDS)) {
							try {
								success.incrementAndGet();
								try {
									Thread.sleep(1);
									if (map.putIfAbsent(lockName, lockName) != null)
										error.incrementAndGet();
									if (!map.remove(lockName, lockName))
										error.incrementAndGet();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							} finally {
								redlock.unlock(lockName);
							}
						} else {
							failed.incrementAndGet();
						}
					} catch (Exception e) {
						error.incrementAndGet();
						e.printStackTrace();
					}

				}
				cdl.countDown();
			});
		}
		cdl.await();
		time = System.currentTimeMillis() - time;
		int count = success.get() + failed.get();
		System.out.println("completed " + count + " requests with concurrency(" + THREADS + ") in " + time
				+ "ms (tps = " + (count * 1000 / time) + "), success=" + success.get() + ", failed=" + failed.get());
		assertThat(error.get(), is(0));
	}

	@Configuration
	static class RedlockServiceConfiguration {

		@Bean
		public Redlock redlock() {
			return new Redlock();
		}

	}

}
