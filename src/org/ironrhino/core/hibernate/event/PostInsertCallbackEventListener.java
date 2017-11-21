package org.ironrhino.core.hibernate.event;

import javax.persistence.PostPersist;

import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.util.ReflectionUtils;

public class PostInsertCallbackEventListener implements PostInsertEventListener {

	private static final long serialVersionUID = 857354753352009583L;

	@Override
	public void onPostInsert(PostInsertEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostPersist.class);
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister paramEntityPersister) {
		return false;
	}

}