package org.ironrhino.core.aop;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
public class PublishAspect extends TransactionSynchronizationAdapter implements Ordered {

	private static final String HIBERNATE_EVENTS = "HIBERNATE_EVENTS_FOR_PUBLISH";

	@Autowired
	private EventPublisher eventPublisher;

	private int order;

	public PublishAspect() {
		order = 1;
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	protected boolean isBypass() {
		return AopContext.isBypass(this.getClass());
	}

	@Before("execution(public * *(..)) and @annotation(transactional)")
	public void registerTransactionSyncrhonization(JoinPoint jp, Transactional transactional) {
		if (!isBypass() && !transactional.readOnly())
			TransactionSynchronizationManager.registerSynchronization(this);
	}

	@Override
	public void afterCommit() {
		List<AbstractEvent> events = getHibernateEvents(false);
		if (events == null || events.isEmpty())
			return;
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
			PublishAware publishAware = ReflectionUtils.getActualClass(entity).getAnnotation(PublishAware.class);
			if (publishAware != null)
				eventPublisher.publish(new EntityOperationEvent<>((Persistable<?>) entity, action),
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
