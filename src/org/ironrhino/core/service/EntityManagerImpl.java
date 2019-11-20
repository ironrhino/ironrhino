package org.ironrhino.core.service;

import org.ironrhino.core.hibernate.HibernateEnabled;
import org.ironrhino.core.model.Persistable;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@HibernateEnabled
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
