package org.ironrhino.core.hibernate;

import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.stereotype.Component;

@Component
public class EventListenerForLifecyle implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener,
		PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener, PostLoadEventListener {

	private static final long serialVersionUID = 857354753352009583L;

	@Override
	public boolean onPreInsert(PreInsertEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PrePersist.class);
		return false;
	}

	@Override
	public boolean onPreUpdate(PreUpdateEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PreUpdate.class);
		return false;
	}

	@Override
	public boolean onPreDelete(PreDeleteEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PreRemove.class);
		return false;
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostPersist.class);
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostUpdate.class);
	}

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostRemove.class);
	}

	@Override
	public void onPostLoad(PostLoadEvent event) {
		ReflectionUtils.processCallback(event.getEntity(), PostLoad.class);
	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister event) {
		return true;
	}

}