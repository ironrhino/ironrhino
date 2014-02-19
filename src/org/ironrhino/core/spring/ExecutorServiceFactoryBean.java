package org.ironrhino.core.spring;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ironrhino.core.util.NameableThreadFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component("executorService")
public class ExecutorServiceFactoryBean implements
		FactoryBean<ExecutorService>, InitializingBean, DisposableBean {

	private ExecutorService executorService;

	@Override
	public void afterPropertiesSet() {
		executorService = Executors
				.newCachedThreadPool(new NameableThreadFactory(
						"executorService"));
	}

	@Override
	public void destroy() {
		executorService.shutdown();
	}

	@Override
	public ExecutorService getObject() throws Exception {
		return executorService;
	}

	@Override
	public Class<? extends ExecutorService> getObjectType() {
		return ExecutorService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}