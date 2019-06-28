package org.ironrhino.core.aop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.ironrhino.core.event.EntityOperationEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.Getter;
import lombok.Setter;

@Aspect
@Component
public class PublishAspect implements TransactionSynchronization, Ordered {

	private static final String HIBERNATE_EVENTS = "HIBERNATE_EVENTS_FOR_PUBLISH";

	@Autowired
	private EventPublisher eventPublisher;

	@Getter
	@Setter
	private int order;

	public PublishAspect() {
		order = 1;
	}

	protected boolean isBypass() {
		return AopContext.isBypass(this.getClass());
	}

	@Before("execution(public * *(..)) and @annotation(transactional)")
	public void registerTransactionSynchronization(JoinPoint jp, Transactional transactional) {
		if (!isBypass() && !transactional.readOnly())
			TransactionSynchronizationManager.registerSynchronization(this);
	}

	@Override
	public void afterCommit() {
		List<AbstractEvent> events = getHibernateEvents(false);
		if (events == null || events.isEmpty())
			return;
		Map<Persistable<?>, EntityOperationType> actions = new HashMap<>();
		for (AbstractEvent event : events) {
			Object entity;
			EntityOperationType action;
			if (event instanceof PostInsertEvent) {
				entity = ((PostInsertEvent) event).getEntity();
				action = EntityOperationType.CREATE;
			} else if (event instanceof PostUpdateEvent) {
				entity = ((PostUpdateEvent) event).getEntity();
				action = EntityOperationType.UPDATE;
			} else if (event instanceof PostDeleteEvent) {
				entity = ((PostDeleteEvent) event).getEntity();
				action = EntityOperationType.DELETE;
			} else {
				continue;
			}
			EntityOperationType previousAction = actions.get(entity);
			if (previousAction == EntityOperationType.CREATE || previousAction == EntityOperationType.DELETE)
				continue;
			actions.put((Persistable<?>) entity, action);
		}
		for (Map.Entry<Persistable<?>, EntityOperationType> entry : actions.entrySet()) {
			PublishAware publishAware = ReflectionUtils.getActualClass(entry.getKey())
					.getAnnotation(PublishAware.class);
			if (publishAware != null)
				eventPublisher.publish(new EntityOperationEvent<>(entry.getKey(), entry.getValue()),
						publishAware.scope());
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (TransactionSynchronizationManager.hasResource(HIBERNATE_EVENTS))
			TransactionSynchronizationManager.unbindResource(HIBERNATE_EVENTS);
	}

	@SuppressWarnings("unchecked")
	public static List<AbstractEvent> getHibernateEvents(boolean create) {
		if (create && !TransactionSynchronizationManager.hasResource(HIBERNATE_EVENTS))
			TransactionSynchronizationManager.bindResource(HIBERNATE_EVENTS, new ArrayList<>());
		return (List<AbstractEvent>) TransactionSynchronizationManager.getResource(HIBERNATE_EVENTS);
	}

}
