package org.ironrhino.core.event;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "applicationEventMulticaster.async", value = "true")
@Component
public class ApplicationEventMulticaster extends SimpleApplicationEventMulticaster {

	@Autowired
	private AsyncTaskExecutor taskExecutor;

	@PostConstruct
	public void afterPropertiesSet() {
		setTaskExecutor(taskExecutor);
	}

}
