package org.ironrhino.core.spring;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.util.NameableThreadFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component("executorService")
public class ExecutorServiceFactoryBean implements FactoryBean<ExecutorService>, InitializingBean, DisposableBean {

	private ExecutorService executorService;

	@Override
	public void afterPropertiesSet() {
		executorService = Executors.newCachedThreadPool(new NameableThreadFactory("executorService"));
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