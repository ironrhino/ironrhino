package org.ironrhino.core.aop;

import org.hibernate.event.spi.PostCommitDeleteEventListener;
import org.hibernate.event.spi.PostCommitInsertEventListener;
import org.hibernate.event.spi.PostCommitUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.stereotype.Component;

@Component
public class EventListenerForPublish
		implements PostCommitInsertEventListener, PostCommitUpdateEventListener, PostCommitDeleteEventListener {

	private static final long serialVersionUID = 9062685218966998574L;

	@Override
	public void onPostDelete(PostDeleteEvent event) {
		if (isAnnotated(event.getEntity()))
			PublishAspect.getHibernateEvents(true).add(event);
	}

	@Override
	public void onPostInsert(PostInsertEvent event) {
		if (isAnnotated(event.getEntity()))
			PublishAspect.getHibernateEvents(true).add(event);
	}

	@Override
	public void onPostUpdate(PostUpdateEvent event) {
		if (isAnnotated(event.getEntity()))
			PublishAspect.getHibernateEvents(true).add(event);
	}

	@Override
	public void onPostDeleteCommitFailed(PostDeleteEvent event) {

	}

	@Override
	public void onPostUpdateCommitFailed(PostUpdateEvent event) {

	}

	@Override
	public void onPostInsertCommitFailed(PostInsertEvent event) {

	}

	@Override
	public boolean requiresPostCommitHanding(EntityPersister persister) {
		return false;
	}

	private static boolean isAnnotated(Object entity) {
		return ReflectionUtils.getActualClass(entity).isAnnotationPresent(PublishAware.class);
	}
}