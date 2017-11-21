package org.ironrhino.core.hibernate.event;

import java.io.Serializable;

import org.hibernate.event.internal.DefaultPersistEventListener;
import org.hibernate.event.spi.EventSource;

public class PersistCallbackEventListener extends DefaultPersistEventListener {

	private static final long serialVersionUID = 3140713698042390301L;

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
