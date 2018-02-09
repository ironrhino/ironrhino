package org.ironrhino.core.hibernate.event;

import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.event.spi.FlushEntityEvent;
import org.hibernate.event.spi.FlushEntityEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.util.ReflectionUtils;

public class SimpleFlushEntityEventListener implements FlushEntityEventListener {

	private static final long serialVersionUID = 2455740016988763934L;

	@Override
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
		EntityEntry entry = event.getEntityEntry();
		EntityPersister persister = entry.getPersister();
		if (persister.isVersioned()) {
			Object[] values = persister.getPropertyValues(event.getEntity());
			if (!entry.getVersion().equals(Versioning.getVersion(values, persister))) {
				throw new StaleObjectStateException(entry.getEntityName(), entry.getId());
			}
		}

		Class<?> clazz = ReflectionUtils.getActualClass(event.getEntity());
		if (clazz.isAnnotationPresent(Immutable.class))
			throw new IllegalStateException(clazz.getName() + " is @" + Immutable.class.getSimpleName());
		if (event.getDirtyProperties() != null && clazz.isAnnotationPresent(AppendOnly.class))
			throw new IllegalStateException(clazz.getName() + " is @" + AppendOnly.class.getSimpleName());
	}

}