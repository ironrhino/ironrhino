package org.ironrhino.core.hibernate.event;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.PersistEventListener;
import org.ironrhino.core.util.ReflectionUtils;

public class SimplePersistEventListener implements PersistEventListener {

	private static final long serialVersionUID = 3140713698042390301L;

	@Override
	public void onPersist(PersistEvent event) throws HibernateException {
		Class<?> clazz = ReflectionUtils.getActualClass(event.getObject());
		if (clazz.isAnnotationPresent(Immutable.class))
			throw new IllegalStateException(clazz.getName() + " is @" + Immutable.class.getSimpleName());
	}

	@Override
	public void onPersist(PersistEvent event, @SuppressWarnings("rawtypes") Map paramMap) throws HibernateException {
		onPersist(event);
	}
}
