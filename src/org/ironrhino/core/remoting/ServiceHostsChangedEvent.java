package org.ironrhino.core.remoting;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class ServiceHostsChangedEvent extends ApplicationEvent {

	private static final long serialVersionUID = 5230508615882925303L;

	private String serviceName;

	public ServiceHostsChangedEvent(String serviceName) {
		super(serviceName);
		this.serviceName = serviceName;
	}

}
