package org.ironrhino.core.hibernate.event;

import java.io.Serializable;

import javax.persistence.PrePersist;

import org.hibernate.annotations.Immutable;
import org.hibernate.event.internal.DefaultSaveEventListener;
import org.hibernate.event.spi.EventSource;
import org.ironrhino.core.util.ReflectionUtils;

public class SaveCallbackEventListener extends DefaultSaveEventListener {

	private static final long serialVersionUID = 3140713698042390301L;

	@Override
	protected Serializable saveWithRequestedId(Object entity, Serializable requestedId, String entityName,
			Object anything, EventSource source) {
		check(entity);
		return super.saveWithRequestedId(entity, requestedId, entityName, anything, source);
	}

	@Override
	protected Serializable saveWithGeneratedId(Object entity, String entityName, Object anything, EventSource source,
			boolean requiresImmediateIdAccess) {
		check(entity);
		return super.saveWithGeneratedId(entity, entityName, anything, source, requiresImmediateIdAccess);
	}

	static void check(Object entity) {
		Class<?> clazz = ReflectionUtils.getActualClass(entity);
		Immutable immutable = clazz.getAnnotation(Immutable.class);
		if (immutable != null)
			throw new IllegalStateException(clazz + " is @" + Immutable.class.getSimpleName());
		ReflectionUtils.processCallback(entity, PrePersist.class);
	}

}
