package org.ironrhino.common.record;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.hibernate.type.BagType;
import org.hibernate.type.Type;
import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
@Slf4j
public class RecordAspect implements TransactionSynchronization, Ordered {

	private static final String HIBERNATE_EVENTS = "HIBERNATE_EVENTS_FOR_RECORD";

	@Autowired
	private SessionFactory sessionFactory;

	@Getter
	@Setter
	private int order;

	public RecordAspect() {
		order = 1;
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

		Session session = sessionFactory.getCurrentSession();
		boolean needFlush = false;
		for (AbstractEvent event : events) {
			try {
				Object entity;
				EntityOperationType action;
				String payload = null;
				if (event instanceof PostInsertEvent) {
					PostInsertEvent pie = (PostInsertEvent) event;
					entity = pie.getEntity();
					action = EntityOperationType.CREATE;
					EntityMetamodel em = pie.getPersister().getEntityMetamodel();
					String[] propertyNames = em.getPropertyNames();
					Type[] propertyTypes = em.getPropertyTypes();
					StringBuilder sb = new StringBuilder();
					boolean sep = false;
					for (int i = 0; i < propertyNames.length; i++) {
						if (propertyTypes[i].getName().toLowerCase().endsWith("lob"))
							continue;
						Object value = pie.getState()[i];
						if (value == null || value instanceof Collection && ((Collection<?>) value).isEmpty()
								|| value.getClass().isArray() && Array.getLength(value) == 0)
							continue;
						if (sep)
							sb.append("\n------\n");
						sb.append(propertyNames[i]);
						sb.append(": ");
						sb.append(StringUtils.toString(value));
						sep = true;
					}
					payload = sb.toString();
				} else if (event instanceof PostUpdateEvent) {
					PostUpdateEvent pue = (PostUpdateEvent) event;
					entity = pue.getEntity();
					action = EntityOperationType.UPDATE;
					Object[] databaseSnapshot = pue.getOldState();
					Object[] propertyValues = pue.getState();
					int[] dirtyProperties = pue.getDirtyProperties();
					if (databaseSnapshot == null || dirtyProperties == null)
						continue;
					EntityMetamodel em = pue.getPersister().getEntityMetamodel();
					String[] propertyNames = em.getPropertyNames();
					Type[] propertyTypes = em.getPropertyTypes();
					StringBuilder sb = new StringBuilder();
					boolean sep = false;
					for (int i = 0; i < dirtyProperties.length; i++) {
						if (propertyTypes[dirtyProperties[i]].getName().toLowerCase().endsWith("lob"))
							continue;
						String propertyName = propertyNames[dirtyProperties[i]];
						IdentifierGenerator ig = em.getIdentifierProperty().getIdentifierGenerator();
						if (ig instanceof ForeignGenerator) {
							if (propertyName.equals(((ForeignGenerator) ig).getPropertyName()))
								continue; // @MapsId
						}
						Object oldValue = databaseSnapshot[dirtyProperties[i]];
						Object newValue = propertyValues[dirtyProperties[i]];
						if (oldValue instanceof Persistable)
							oldValue = ((Persistable<?>) oldValue).getId();
						if (newValue instanceof Persistable)
							newValue = ((Persistable<?>) newValue).getId();
						if (em.getPropertyTypes()[dirtyProperties[i]] instanceof BagType) {
							if (oldValue instanceof Collection) {
								List<Object> list = new ArrayList<>();
								for (Object o : (Collection<?>) oldValue)
									list.add(o instanceof Persistable<?> ? ((Persistable<?>) o).getId()
											: String.valueOf(o));
								oldValue = list;
							}
							if (newValue instanceof Collection) {
								List<Object> list = new ArrayList<>();
								for (Object o : (Collection<?>) newValue)
									list.add(o instanceof Persistable<?> ? ((Persistable<?>) o).getId()
											: String.valueOf(o));
								newValue = list;
							}
						}
						if (Objects.equals(oldValue, newValue))
							continue;
						if (oldValue instanceof Collection && ((Collection<?>) oldValue).isEmpty() && newValue == null
								|| newValue instanceof Collection && ((Collection<?>) newValue).isEmpty()
										&& oldValue == null)
							continue;
						if (sep)
							sb.append("\n------\n");
						sb.append(propertyName);
						sb.append(": ");
						sb.append(StringUtils.toString(oldValue));
						sb.append(" -> ");
						sb.append(StringUtils.toString(newValue));
						sep = true;
					}
					payload = sb.toString();
					if (payload.isEmpty())
						continue;
				} else if (event instanceof PostDeleteEvent) {
					entity = ((PostDeleteEvent) event).getEntity();
					action = EntityOperationType.DELETE;
				} else {
					continue;
				}
				session.evict(entity);
				Record record = new Record();
				UserDetails ud = AuthzUtils.getUserDetails();
				if (ud != null) {
					record.setOperatorId(ud.getUsername());
					record.setOperatorClass(ud.getClass().getName());
				}
				record.setEntityId(String.valueOf(((Persistable<?>) entity).getId()));
				record.setEntityClass(ReflectionUtils.getActualClass(entity).getName());
				record.setEntityToString(payload != null ? payload : entity.toString());
				record.setAction(action.name());
				record.setRecordDate(new Date());
				session.save(record);
				needFlush = true;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		if (needFlush)
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
