package org.ironrhino.core.event;

import org.ironrhino.core.model.Persistable;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

import lombok.Getter;

public class EntityOperationEvent<T extends Persistable<?>> extends BaseEvent<T> implements ResolvableTypeProvider {

	private static final long serialVersionUID = -3336231774669978161L;

	@Getter
	private final EntityOperationType type;

	public EntityOperationEvent(T entity, EntityOperationType type) {
		super(entity);
		this.type = type;
	}

	public T getEntity() {
		return getSource();
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(getSource()));
	}

	@Override
	public String toString() {
		return getClass().getName() + "[type=" + type + ", source=" + source.getClass().getName() + "(" + source + ")]";
	}
}
