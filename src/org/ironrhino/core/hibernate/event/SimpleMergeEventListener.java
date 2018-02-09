package org.ironrhino.core.hibernate.event;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Immutable;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.MergeEventListener;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.util.ReflectionUtils;

public class SimpleMergeEventListener implements MergeEventListener {

	private static final long serialVersionUID = 3140713698042390301L;

	@Override
	public void onMerge(MergeEvent event) throws HibernateException {
		Class<?> clazz = ReflectionUtils.getActualClass(event.getEntity());
		if (clazz.isAnnotationPresent(Immutable.class))
			throw new IllegalStateException(clazz.getName() + " is @" + Immutable.class.getSimpleName());
		if (clazz.isAnnotationPresent(AppendOnly.class))
			throw new IllegalStateException(clazz.getName() + " is @" + AppendOnly.class.getSimpleName());
	}

	@Override
	public void onMerge(MergeEvent event, @SuppressWarnings("rawtypes") Map paramMap) throws HibernateException {
		onMerge(event);
	}
}
