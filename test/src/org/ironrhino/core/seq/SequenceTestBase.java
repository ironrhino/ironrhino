package org.ironrhino.core.seq;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.util.DateUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class SequenceTestBase {

	public static final int THREADS = 50;

	public static final int LOOP = 10000;

	private static ExecutorService executorService;

	@Autowired
	private Sequence sample1Sequence;

	@Autowired
	private Sequence sample2Sequence;

	@BeforeClass
	public static void setup() {
		executorService = Executors.newFixedThreadPool(THREADS);
	}

	@AfterClass
	public static void destroy() {
		executorService.shutdown();
	}

	@Test
	public void testSimple() throws InterruptedException {
		test(false);
	}

	@Test
	public void testCyclic() throws InterruptedException {
		test(true);
	}

	private void test(boolean cyclic) throws InterruptedException {
		final ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<>(THREADS * LOOP * 2);
		final CountDownLatch cdl = new CountDownLatch(THREADS);
		final AtomicInteger count = new AtomicInteger();
		final Sequence seq = cyclic ? sample2Sequence : sample1Sequence;
		long time = System.currentTimeMillis();
		for (int i = 0; i < THREADS; i++) {
			executorService.execute(() -> {

				for (int j = 0; j < LOOP; j++) {
					try {
						String id = seq.nextStringValue();
						Long time2 = System.currentTimeMillis();
						Long old = map.putIfAbsent(id, time2);
						if (old != null)
							System.out.println(id + " , old=" + DateUtils.formatTimestamp(new Date(old)) + " , new="
									+ DateUtils.formatTimestamp(new Date(time2)));
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
		time = System.currentTimeMillis() - time;
		System.out.println(
				"completed " + count.get() + " requests with concurrency(" + THREADS + ") in " + time + "ms (tps = "
						+ (int) (((double) count.get() / time) * 1000) + ") using " + seq.getClass().getSimpleName());
		assertThat(map.size(), is(LOOP * THREADS));
	}

}
