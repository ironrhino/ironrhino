package org.ironrhino.core.service;

import java.io.Serializable;

import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Primary
@Service
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class EntityManagerImpl<PK extends Serializable, T extends Persistable<PK>> extends BaseManagerImpl<PK, T>
		implements EntityManager<PK, T> {

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
