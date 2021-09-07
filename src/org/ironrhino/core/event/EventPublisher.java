package org.ironrhino.core.event;

import org.ironrhino.core.metadata.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

	@Autowired
	private ApplicationContext ctx;

	@Autowired(required = false)
	private ApplicationEventTopic applicationEventTopic;

	public void publish(ApplicationEvent event, final Scope scope) {
		if (applicationEventTopic != null && scope != null && scope != Scope.LOCAL)
			applicationEventTopic.publish(event, scope);
		else
			ctx.publishEvent(event);
	}

	@EventListener
	public void onApplicationEvent(ApplicationContextEvent event) {
		if (event.getApplicationContext() != ctx)
			return;
		if (event instanceof ContextRefreshedEvent) {
			publish(new InstanceStartupEvent(), Scope.GLOBAL);
		} else if (event instanceof ContextClosedEvent) {
			publish(new InstanceShutdownEvent(), Scope.GLOBAL);
		}
	}

}
