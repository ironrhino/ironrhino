package org.ironrhino.core.event;

import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "applicationEventMulticaster.async", value = "true")
@Component
public class ApplicationEventMulticaster extends SimpleApplicationEventMulticaster {

	@Autowired(required = false)
	@Qualifier("executorService")
	private ExecutorService taskExecutor;

	@PostConstruct
	public void afterPropertiesSet() {
		setTaskExecutor(taskExecutor);
	}

}
