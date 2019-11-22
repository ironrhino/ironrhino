package org.ironrhino.core.event;

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
	public String toString() {
		if ("".equals(source))
			return getClass().getName();
		return getClass().getName() + "[source=" + source + "]";
	}

}
