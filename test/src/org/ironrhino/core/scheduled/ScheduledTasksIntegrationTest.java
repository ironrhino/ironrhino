package org.ironrhino.core.scheduled;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.ironrhino.core.scheduled.ScheduledTaskRegistry.ScheduledTask;
import org.ironrhino.core.scheduled.ScheduledTaskRegistry.ScheduledType;
import org.ironrhino.core.scheduled.impl.StandaloneScheduledTaskCircuitBreaker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ScheduledTasksIntegrationTest.Config.class)
public class ScheduledTasksIntegrationTest {

	private static final int STARTED = 0;
	private static final int ENDED = 1;

	private static ReentrantLock lock = new ReentrantLock();
	private static Condition start = lock.newCondition();
	private static Condition end = lock.newCondition();
	private static volatile int status = ENDED;
	private static volatile int counter = 0;
	private static volatile boolean exception = false;

	@Autowired
	private ScheduledTaskRegistry taskRegistry;

	@Autowired
	private ScheduledTaskCircuitBreaker circuitBreaker;

	@Test
	public void testScheduledTaskRegistry() {
		List<ScheduledTask> tasks = taskRegistry.getTasks();
		assertThat(tasks, is(notNullValue()));
		assertThat(tasks.size(), is(3));

		assertThat(tasks.get(0).getType(), is(ScheduledType.CRON));
		assertThat(tasks.get(0).getCron(), is("0 * * * * *"));

		assertThat(tasks.get(1).getType(), is(ScheduledType.FIXEDDELAY));
		assertThat(tasks.get(1).getInitialDelay(), is(10L));
		assertThat(tasks.get(1).getInterval(), is(50L));

		assertThat(tasks.get(2).getType(), is(ScheduledType.FIXEDRATE));
		assertThat(tasks.get(2).getInitialDelay(), is(100L));
		assertThat(tasks.get(2).getInterval(), is(500L));
	}

	@Test(timeout = 5000)
	public void testShortCircuit() throws InterruptedException, BrokenBarrierException {
		String task = "scheduledTasks.fixedDelayTask()";
		for (int i = 0; i < 5; i++) {
			scheduleControl(i, task, false);
		}

		scheduleControl(5, task, true);

		lock.lock();
		try {
			assertThat(start.await(100, TimeUnit.MILLISECONDS), is(false));
			assertThat(status, is(ENDED));
			assertThat(exception, is(true));
		} finally {
			lock.unlock();
		}

		for (int i = 6; i < 10; i++) {
			scheduleControl(i, task, false);
		}

	}

	private void scheduleControl(int expectedCounter, String task, boolean shortCircuit) throws InterruptedException {
		if (!shortCircuit) {
			circuitBreaker.setShortCircuit(task, false);
		}
		lock.lock();
		try {
			while (status != STARTED) {
				start.await();
			}
			assertThat(counter, is(expectedCounter));
			if (shortCircuit) {
				circuitBreaker.setShortCircuit(task, true);
			}

			end.signal();
			status = ENDED;
		} finally {
			lock.unlock();
		}
	}

	@Component("scheduledTasks")
	@EnableScheduling
	static class ScheduledTasks {

		@Scheduled(cron = "0 * * * * *")
		public void cronTask() {

		}

		@Scheduled(initialDelay = 10, fixedDelay = 50)
		public void fixedDelayTask() throws InterruptedException {
			lock.lock();
			try {
				start.signal();
				status = STARTED;

				while (status != ENDED) {
					end.await();
				}
			} finally {
				counter++;
				lock.unlock();
			}
		}

		@Scheduled(initialDelay = 100, fixedRate = 500)
		public void fixedRateTask() {

		}

	}

	@ComponentScan
	@Configuration
	@EnableAspectJAutoProxy(proxyTargetClass = true)
	static class Config implements SchedulingConfigurer {

		@Bean
		public TaskScheduler taskScheduler() {
			ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
			threadPoolTaskScheduler.setPoolSize(2);
			threadPoolTaskScheduler.setThreadNamePrefix("taskScheduler-");
			threadPoolTaskScheduler.setErrorHandler(ex -> {
				if (ex.getMessage().contains("scheduledTasks.fixedDelayTask()")) {
					exception = true;
				}
			});
			return threadPoolTaskScheduler;
		}

		@Bean
		public static ScheduledTaskCircuitBreakerAspect circuitBreakerAspect() {
			return new ScheduledTaskCircuitBreakerAspect();
		}

		@Bean
		public ScheduledTaskRegistry taskRegistry() {
			return new ScheduledTaskRegistry();
		}

		@Bean
		public ScheduledTaskCircuitBreaker scheduledTaskCircuitBreaker() {
			return new StandaloneScheduledTaskCircuitBreaker();
		}

		@Override
		public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
			taskRegistrar.setTaskScheduler(taskScheduler());
		}
	}

}
