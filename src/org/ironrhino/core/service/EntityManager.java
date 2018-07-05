package org.ironrhino.core.service;

import java.io.Serializable;

import org.ironrhino.core.model.Persistable;

public interface EntityManager<PK extends Serializable, T extends Persistable<PK>> extends BaseManager<PK, T> {

	void setEntityClass(Class<T> clazz);

}
