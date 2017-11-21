package org.ironrhino.core.hibernate.event;

import java.io.Serializable;

import org.hibernate.event.internal.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.spi.EventSource;

public class SaveOrUpdateCallbackEventListener extends DefaultSaveOrUpdateEventListener {

	private static final long serialVersionUID = 4523243049333324464L;

	@Override
	protected Serializable saveWithRequestedId(Object entity, Serializable requestedId, String entityName,
			Object anything, EventSource source) {
		SaveCallbackEventListener.check(entity);
		return super.saveWithRequestedId(entity, requestedId, entityName, anything, source);
	}

	@Override
	protected Serializable saveWithGeneratedId(Object entity, String entityName, Object anything, EventSource source,
			boolean requiresImmediateIdAccess) {
		SaveCallbackEventListener.check(entity);
		return super.saveWithGeneratedId(entity, entityName, anything, source, requiresImmediateIdAccess);
	}
}
