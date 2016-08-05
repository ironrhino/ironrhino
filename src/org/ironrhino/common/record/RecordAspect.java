package org.ironrhino.common.record;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class RecordAspect extends TransactionSynchronizationAdapter implements Ordered {

	private static final String HIBERNATE_EVENTS = "HIBERNATE_EVENTS";

	@Autowired
	private Logger logger;

	@Autowired
	private SessionFactory sessionFactory;

	private int order;

	public RecordAspect() {
		order = 1;
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Before("execution(public * *(..)) and @annotation(transactional)")
	public void registerTransactionSyncrhonization(JoinPoint jp, Transactional transactional) {
		if (!transactional.readOnly())
			TransactionSynchronizationManager.registerSynchronization(this);
	}

	@Override
	public void afterCommit() {
		List<AbstractEvent> events = getHibernateEvents(false);
		if (events == null || events.isEmpty())
			return;

		Session session = sessionFactory.getCurrentSession();
		for (AbstractEvent event : events) {
			try {
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

				Record record = new Record();
				UserDetails ud = AuthzUtils.getUserDetails();
				if (ud != null) {
					record.setOperatorId(ud.getUsername());
					record.setOperatorClass(ud.getClass().getName());
				}
				record.setEntityId(String.valueOf(((Persistable<?>) entity).getId()));
				record.setEntityClass(ReflectionUtils.getActualClass(entity).getName());
				record.setEntityToString(entity.toString());
				record.setAction(action.name());
				record.setRecordDate(new Date());
				session.save(record);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		session.flush();
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
