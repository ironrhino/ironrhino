package org.ironrhino.core.event;

import org.ironrhino.core.metadata.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

	@Autowired
	private ApplicationEventPublisher publisher;

	@Autowired(required = false)
	private ApplicationEventTopic applicationEventTopic;

	public void publish(final ApplicationEvent event, final Scope scope) {
		if (applicationEventTopic != null && scope != null && scope != Scope.LOCAL)
			applicationEventTopic.publish(event, scope);
		else
			publisher.publishEvent(event);
	}

	@EventListener
	public void onApplicationEvent(ApplicationContextEvent event) {
		if (event.getApplicationContext().getParent() != null)
			return;
		if (event instanceof ContextRefreshedEvent) {
			InstanceStartupEvent ise = new InstanceStartupEvent();
			if (applicationEventTopic != null)
				applicationEventTopic.publish(ise, Scope.GLOBAL);
			else
				publisher.publishEvent(ise);
		} else if (event instanceof ContextClosedEvent) {
			InstanceShutdownEvent ise = new InstanceShutdownEvent();
			if (applicationEventTopic != null)
				applicationEventTopic.publish(ise, Scope.GLOBAL);
			else
				publisher.publishEvent(ise);
		}
	}

}
