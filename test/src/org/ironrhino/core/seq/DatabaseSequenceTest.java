package org.ironrhino.core.seq;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.sequence.Sequence;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "ctx.xml" })
public class DatabaseSequenceTest {

	public static final int THREADS = 20;

	public static final int LOOP = 10000;

	private static ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<>(THREADS * LOOP * 2);

	private static ExecutorService executorService;

	@Autowired
	@Qualifier("sampleSequence")
	private Sequence sampleSequence;

	@BeforeClass
	public static void setup() {
		executorService = Executors.newFixedThreadPool(THREADS);
	}

	@AfterClass
	public static void destroy() {
		executorService.shutdown();
	}

	@Test
	public void test() throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(THREADS);
		final AtomicInteger count = new AtomicInteger();
		long time = System.currentTimeMillis();
		for (int i = 0; i < THREADS; i++) {
			executorService.execute(() -> {
				for (int j = 0; j < LOOP; j++) {
					try {
						String id = sampleSequence.nextStringValue();
						Long time2 = System.currentTimeMillis();
						Long old = map.putIfAbsent(id, time2);
						if (old != null)
							System.out.println(id + " , old=" + old + " , new=" + time2);
						else
							count.incrementAndGet();
					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				cdl.countDown();
			});
		}
		cdl.await();
		System.out.println("completed " + count.get() + " requests in " + (System.currentTimeMillis() - time) + "ms");
		assertEquals(LOOP * THREADS, map.size());
	}

}
