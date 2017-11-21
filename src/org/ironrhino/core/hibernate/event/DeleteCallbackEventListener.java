package org.ironrhino.core.hibernate.event;

import javax.persistence.PreRemove;

import org.hibernate.event.internal.DefaultDeleteEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.util.ReflectionUtils;

public class DeleteCallbackEventListener extends DefaultDeleteEventListener {

	private static final long serialVersionUID = 995717774081529921L;

	@Override
	protected boolean invokeDeleteLifecycle(EventSource session, Object entity, EntityPersister persister) {
		ReflectionUtils.processCallback(entity, PreRemove.class);
		return super.invokeDeleteLifecycle(session, entity, persister);
	}

}
