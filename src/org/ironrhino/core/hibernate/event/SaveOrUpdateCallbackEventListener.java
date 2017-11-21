package org.ironrhino.core.hibernate.event;

import java.io.Serializable;

import javax.persistence.PrePersist;

import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.spi.EventSource;
import org.ironrhino.core.util.ReflectionUtils;

public class SaveOrUpdateCallbackEventListener extends DefaultSaveOrUpdateEventListener {

	private static final long serialVersionUID = 4523243049333324464L;

	@Override
	protected Serializable saveWithRequestedId(Object entity, Serializable requestedId, String entityName,
			Object anything, EventSource source) {
		ReflectionUtils.processCallback(entity, PrePersist.class);
		return super.saveWithRequestedId(entity, requestedId, entityName, anything, source);
	}

	@Override
	protected Serializable saveWithGeneratedId(Object entity, String entityName, Object anything, EventSource source,
			boolean requiresImmediateIdAccess) {
		ReflectionUtils.processCallback(entity, PrePersist.class);
		return super.saveWithGeneratedId(entity, entityName, anything, source, requiresImmediateIdAccess);
	}
}
