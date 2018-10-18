package org.ironrhino.core.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

public class PasswordChangedEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = 7854178610021098541L;

	public PasswordChangedEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

}
