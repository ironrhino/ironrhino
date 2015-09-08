package org.ironrhino.core.spring.configuration;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.ironrhino.core.util.NameableThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@EnableScheduling
@EnableAsync(order = -999, proxyTargetClass = true)
@Configuration
public class SchedulingConfiguration implements SchedulingConfigurer, AsyncConfigurer {

	@Value("${taskScheduler.poolSize:5}")
	private int taskSchedulerPoolSize = 5;

	@Value("${taskExecutor.poolSize:5}")
	private int taskExecutorPoolSize = 5;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());
	}

	@Override
	public Executor getAsyncExecutor() {
		return taskExecutorThreadPool();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		return new ConcurrentTaskScheduler(taskSchedulerThreadPool());
	}

	@Bean(destroyMethod = "shutdown")
	public ScheduledExecutorService taskSchedulerThreadPool() {
		return Executors.newScheduledThreadPool(taskSchedulerPoolSize, new NameableThreadFactory("taskScheduler"));
	}

	@Bean(destroyMethod = "shutdown")
	public ExecutorService taskExecutorThreadPool() {
		return Executors.newFixedThreadPool(taskExecutorPoolSize, new NameableThreadFactory("taskExecutor"));
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return new AsyncUncaughtExceptionHandler() {

			private Logger logger = LoggerFactory.getLogger(AsyncUncaughtExceptionHandler.class);

			@Override
			public void handleUncaughtException(Throwable ex, Method method, Object... args) {
				logger.error("method ( " + method.toString() + " ) error", ex);
			}

		};
	}

}
