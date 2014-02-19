package org.ironrhino.common.record;

import java.util.Date;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import org.ironrhino.core.aop.AopContext;
import org.ironrhino.core.event.EntityOperationType;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.AuthzUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class RecordAspect implements Ordered {
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private SessionFactory sessionFactory;

	public RecordAspect() {
		order = 1;
	}

	private int order;

	@AfterReturning(pointcut = "execution(java.util.List org.ironrhino.core.service.BaseManager.delete(*)) ", returning = "list")
	@SuppressWarnings("rawtypes")
	public void deleteBatch(List list) throws Throwable {
		if (!AopContext.isBypass(this.getClass()) && list != null)
			for (Object entity : list) {
				RecordAware recordAware = entity.getClass().getAnnotation(
						RecordAware.class);
				if (recordAware != null)
					record((Persistable) entity, EntityOperationType.DELETE);
			}
	}

	@Around("execution(* org.ironrhino.core.service.BaseManager.save(*)) and args(entity) and @args(recordAware)")
	public Object save(ProceedingJoinPoint call, Persistable<?> entity,
			RecordAware recordAware) throws Throwable {
		boolean isNew = entity.isNew();
		Object result = call.proceed();
		if (!AopContext.isBypass(this.getClass()))
			record(entity, isNew ? EntityOperationType.CREATE
					: EntityOperationType.UPDATE);
		return result;
	}

	@AfterReturning("execution(* org.ironrhino.core.service.BaseManager.delete(*)) and args(entity) and @args(recordAware)")
	public void delete(Persistable<?> entity, RecordAware recordAware) {
		if (!AopContext.isBypass(this.getClass()))
			record(entity, EntityOperationType.DELETE);
	}

	// record to database,may change to use logger system
	private void record(Persistable<?> entity, EntityOperationType action) {
		try {
			Record record = new Record();
			UserDetails ud = AuthzUtils.getUserDetails();
			if (ud != null) {
				record.setOperatorId(ud.getUsername());
				record.setOperatorClass(ud.getClass().getName());
			}

			record.setEntityId(String.valueOf(entity.getId()));
			record.setEntityClass(entity.getClass().getName());
			record.setEntityToString(entity.toString());
			record.setAction(action.name());
			record.setRecordDate(new Date());
			sessionFactory.getCurrentSession().save(record);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
