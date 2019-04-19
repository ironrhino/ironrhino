package org.ironrhino.core.spring;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.util.NameableThreadFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component("executorService")
@Slf4j
public class ExecutorServiceFactoryBean implements FactoryBean<ExecutorService>, InitializingBean, DisposableBean {

	@Value("${executorService.corePoolSize:1}")
	private int corePoolSize = 1;

	@Value("${executorService.maximumPoolSize:100}")
	private int maximumPoolSize = 100;

	@Value("${executorService.keepAliveTime:60}")
	private long keepAliveTime = 60;

	@Value("${executorService.workQueueCapacity:1000000}")
	private int workQueueCapacity = 1000000;

	private ExecutorService executorService;

	@Override
	public void afterPropertiesSet() {
		executorService = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(workQueueCapacity), new NameableThreadFactory("executorService"),
				(runnable, executor) -> {
					log.error("{} is rejected: {}", runnable, executor);
				});
	}

	@Override
	public void destroy() throws InterruptedException {
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);
	}

	@Override
	public ExecutorService getObject() throws Exception {
		return executorService;
	}

	@Override
	public Class<? extends ExecutorService> getObjectType() {
		return ExecutorService.class;
	}

}