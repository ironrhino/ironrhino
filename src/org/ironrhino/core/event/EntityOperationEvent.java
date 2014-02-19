package org.ironrhino.core.event;

import org.ironrhino.core.model.Persistable;

public class EntityOperationEvent extends BaseEvent<Persistable<?>> {

	private static final long serialVersionUID = -3336231774669978161L;

	private EntityOperationType type;

	public EntityOperationEvent(Persistable<?> entity, EntityOperationType type) {
		super(entity);
		this.type = type;
	}

	public Persistable<?> getEntity() {
		return getSource();
	}

	public EntityOperationType getType() {
		return type;
	}

}
