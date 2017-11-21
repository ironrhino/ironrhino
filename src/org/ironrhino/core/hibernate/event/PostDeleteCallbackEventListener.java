package org.ironrhino.core.hibernate.event;

import javax.persistence.PostRemove;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.util.ReflectionUtils;

public class PostDeleteCallbackEventListener implements PostDeleteEventListener {

	private static final long serialVersionUID = 857354753352009583L;

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostRemove.class);
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister paramEntityPersister) {
		return false;
	}

}