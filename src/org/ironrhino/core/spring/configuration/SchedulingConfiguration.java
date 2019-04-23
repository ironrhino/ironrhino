package org.ironrhino.core.spring.configuration;

import java.util.concurrent.Executor;

import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Order(0)
@EnableScheduling
@EnableAsync(order = -999, proxyTargetClass = true)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class SchedulingConfiguration implements SchedulingConfigurer, AsyncConfigurer {

	@Value("${taskScheduler.poolSize:5}")
	private int taskSchedulerPoolSize = 5;

	@Value("${taskExecutor.corePoolSize:50}")
	private int taskExecutorCorePoolSize = 50;

	@Value("${taskExecutor.maxPoolSize:100}")
	private int taskExecutorMaxPoolSize = 100;

	@Value("${taskExecutor.queueCapacity:10000}")
	private int taskExecutorQueueCapacity = 10000;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setTaskScheduler(taskScheduler());
	}

	@Override
	public Executor getAsyncExecutor() {
		return taskExecutor();
	}

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
		threadPoolTaskScheduler.setPoolSize(taskSchedulerPoolSize);
		threadPoolTaskScheduler.setThreadNamePrefix("taskScheduler-");
		return threadPoolTaskScheduler;
	}

	@Bean
	public AsyncTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(taskExecutorCorePoolSize);
		executor.setMaxPoolSize(taskExecutorMaxPoolSize);
		executor.setQueueCapacity(taskExecutorQueueCapacity);
		executor.setThreadNamePrefix("taskExecutor-");
		executor.setAllowCoreThreadTimeOut(true);
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, args) -> {
			LoggerFactory.getLogger(AsyncUncaughtExceptionHandler.class)
					.error("method ( " + method.toString() + " ) error", ex);
		};
	}

}
