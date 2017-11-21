package org.ironrhino.core.hibernate.event;

import javax.persistence.PostUpdate;

import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.util.ReflectionUtils;

public class PostUpdateCallbackEventListener implements PostUpdateEventListener {

	private static final long serialVersionUID = 857354753352009583L;

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostUpdate.class);
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister paramEntityPersister) {
		return false;
	}

}