package org.ironrhino.core.service;

import org.hibernate.SessionFactory;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@BeanPresentConditional(type = SessionFactory.class)
public class EntityManagerImpl<T extends Persistable<?>> extends BaseManagerImpl<T> implements EntityManager<T> {

	private ThreadLocal<Class<T>> entityClassHolder = new ThreadLocal<>();

	@Override
	public void setEntityClass(Class<T> clazz) {
		entityClassHolder.set(clazz);
	}

	@Override
	public Class<T> getEntityClass() {
		return entityClassHolder.get();
	}

}
