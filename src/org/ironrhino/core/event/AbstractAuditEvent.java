package org.ironrhino.core.event;

import org.ironrhino.core.event.BaseEvent;

public abstract class AbstractAuditEvent extends BaseEvent<String> {

	private static final long serialVersionUID = 2656926225727363987L;

	private String remoteAddr;

	public AbstractAuditEvent(String username, String remoteAddr) {
		super(username);
		this.remoteAddr = remoteAddr;
	}

	public String getUsername() {
		return getSource();
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public String[] getArguments() {
		return null;
	}

}
