package org.ironrhino.core.hibernate.event;

import java.io.Serializable;

import javax.persistence.PrePersist;

import org.hibernate.event.internal.DefaultMergeEventListener;
import org.hibernate.event.spi.EventSource;
import org.ironrhino.core.util.ReflectionUtils;

public class MergeCallbackEventListener extends DefaultMergeEventListener {

	private static final long serialVersionUID = 3140713698042390301L;

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
