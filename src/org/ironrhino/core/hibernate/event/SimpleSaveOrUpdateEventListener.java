package org.ironrhino.core.hibernate.event;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.event.spi.SaveOrUpdateEventListener;
import org.ironrhino.core.util.ReflectionUtils;

public class SimpleSaveOrUpdateEventListener implements SaveOrUpdateEventListener {

	private static final long serialVersionUID = 4523243049333324464L;

	@Override
	public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
		Object entity = event.getEntity();
		if (entity == null)
			entity = event.getObject();
		Class<?> clazz = ReflectionUtils.getActualClass(entity);
		if (clazz.isAnnotationPresent(Immutable.class))
			throw new IllegalStateException(clazz.getName() + " is @" + Immutable.class.getSimpleName());
	}

}
