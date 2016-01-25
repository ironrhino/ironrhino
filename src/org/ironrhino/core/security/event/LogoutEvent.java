package org.ironrhino.core.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

public class LogoutEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	public LogoutEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

}
