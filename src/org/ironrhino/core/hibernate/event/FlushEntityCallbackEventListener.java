package org.ironrhino.core.hibernate.event;

import javax.persistence.PreUpdate;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Immutable;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.internal.DefaultFlushEntityEventListener;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.util.ReflectionUtils;

public class FlushEntityCallbackEventListener extends DefaultFlushEntityEventListener {

	private static final long serialVersionUID = 2455740016988763934L;

	@Override
	protected boolean invokeInterceptor(SessionImplementor session, Object entity, EntityEntry entry, Object[] values,
			EntityPersister persister) {
		boolean isDirty = false;
		if (entry.getStatus() != Status.DELETED) {
			Class<?> clazz = ReflectionUtils.getActualClass(entity);
			if (clazz.getAnnotation(Immutable.class) != null)
				throw new IllegalStateException(clazz + " is @" + Immutable.class.getSimpleName());
			if (clazz.getAnnotation(AppendOnly.class) != null)
				throw new IllegalStateException(clazz + " is @" + AppendOnly.class.getSimpleName());
			ReflectionUtils.processCallback(entity, PreUpdate.class);
			isDirty = copyState(entity, persister.getPropertyTypes(), values, session.getFactory());
		}
		return super.invokeInterceptor(session, entity, entry, values, persister) || isDirty;
	}

	private boolean copyState(Object entity, Type[] types, Object[] state, SessionFactory sf) {
		// copy the entity state into the state array and return true if the state has
		// changed
		@SuppressWarnings("deprecation")
		ClassMetadata metadata = sf.getClassMetadata(entity.getClass());
		Object[] newState = metadata.getPropertyValues(entity);
		int size = newState.length;
		boolean isDirty = false;
		for (int index = 0; index < size; index++) {
			if ((state[index] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					&& newState[index] != LazyPropertyInitializer.UNFETCHED_PROPERTY)
					|| (state[index] != newState[index] && !types[index].isEqual(state[index], newState[index]))) {
				isDirty = true;
				state[index] = newState[index];
			}
		}
		return isDirty;
	}
}