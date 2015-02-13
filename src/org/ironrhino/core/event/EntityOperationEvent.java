package org.ironrhino.core.event;

import org.ironrhino.core.model.Persistable;

public class EntityOperationEvent<T extends Persistable<?>> extends
		BaseEvent<T> {

	private static final long serialVersionUID = -3336231774669978161L;

	private EntityOperationType type;

	public EntityOperationEvent(T entity, EntityOperationType type) {
		super(entity);
		this.type = type;
	}

	public T getEntity() {
		return getSource();
	}

	public EntityOperationType getType() {
		return type;
	}

}
