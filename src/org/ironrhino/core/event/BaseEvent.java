package org.ironrhino.core.event;

import java.util.Objects;

import org.ironrhino.core.util.AppInfo;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

public class BaseEvent<T> extends ApplicationEvent {

	private static final long serialVersionUID = -2892858943541156897L;

	@Getter
	private String instanceId = AppInfo.getInstanceId();

	@Getter
	protected T source;

	public BaseEvent(T source) {
		super(source);
		this.source = source;
	}

	public boolean isLocal() {
		return getInstanceId().equals(AppInfo.getInstanceId());
	}

	@Override
	public boolean equals(Object that) {
		if (that == null)
			return false;
		if (this == that)
			return true;
		if (!getClass().isInstance(that))
			return false;
		BaseEvent<?> be = (BaseEvent<?>) that;
		// EventObject.source is transient
		return Objects.equals(this.instanceId, be.instanceId) && Objects.equals(this.source, be.source)
				&& Objects.equals(this.getTimestamp(), be.getTimestamp());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName());
		if (!"".equals(source))
			sb.append("[source=" + source + "]");
		sb.append(" from ").append(getInstanceId());
		return sb.toString();
	}

}
