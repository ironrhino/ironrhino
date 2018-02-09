package org.ironrhino.core.hibernate.event;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.DeleteEventListener;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.util.ReflectionUtils;

public class SimpleDeleteEventListener implements DeleteEventListener {

	private static final long serialVersionUID = 995717774081529921L;

	@Override
	public void onDelete(DeleteEvent event) throws HibernateException {
		Class<?> clazz = ReflectionUtils.getActualClass(event.getObject());
		if (clazz.isAnnotationPresent(Immutable.class))
			throw new IllegalStateException(clazz.getName() + " is @" + Immutable.class.getSimpleName());
		if (clazz.isAnnotationPresent(AppendOnly.class))
			throw new IllegalStateException(clazz.getName() + " is @" + AppendOnly.class.getSimpleName());
	}

	@Override
	public void onDelete(DeleteEvent event, @SuppressWarnings("rawtypes") Set paramSet) throws HibernateException {
		onDelete(event);
	}

}
