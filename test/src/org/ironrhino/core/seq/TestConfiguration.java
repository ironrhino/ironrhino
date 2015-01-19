package org.ironrhino.core.seq;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.ironrhino.core.sequence.Sequence;
import org.ironrhino.core.sequence.CyclicSequence.CycleType;
import org.ironrhino.core.sequence.cyclic.DatabaseCyclicSequenceDelegate;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

	@Bean(autowire = Autowire.BY_NAME)
	public Sequence sampleSequence() {
		DatabaseCyclicSequenceDelegate seq = new DatabaseCyclicSequenceDelegate();
		seq.setCycleType(CycleType.MINUTE);
		return seq;
	}

	@Bean
	public Test test() {
		return new Test();
	}

	public static class Test {

		public static final int THREADS = 20;

		public static final int LOOP = 10000;

		private ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<String, Long>(
				THREADS * LOOP * 2);

		private ExecutorService es = Executors.newFixedThreadPool(THREADS);

		@Autowired
		private Sequence sequence;

		@PostConstruct
		public void init() throws InterruptedException {
			final CountDownLatch cdl = new CountDownLatch(THREADS);
			final AtomicInteger count = new AtomicInteger();
			long time = System.currentTimeMillis();
			for (int i = 0; i < THREADS; i++) {
				es.execute(new Runnable() {

					@Override
					public void run() {
						for (int i = 0; i < LOOP; i++) {
							try {
								String id = sequence.nextStringValue();
								Long time = System.currentTimeMillis();
								Long old = map.putIfAbsent(id, time);
								if (old != null)
									System.out.println(id + " , old=" + old
											+ " , new=" + time);
								else
									count.incrementAndGet();
							} catch (Throwable e) {
								e.printStackTrace();
							}
						}
						cdl.countDown();
					}
				});
			}
			es.shutdown();
			cdl.await();
			System.out.println("completed " + count.get() + " requests in "
					+ (System.currentTimeMillis() - time) + "ms");
		}
	}

}
