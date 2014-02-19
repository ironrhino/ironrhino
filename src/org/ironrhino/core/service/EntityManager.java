package org.ironrhino.core.service;

import org.ironrhino.core.model.Persistable;

public interface EntityManager<T extends Persistable<?>> extends BaseManager<T> {

	public void setEntityClass(Class<T> clazz);

}
