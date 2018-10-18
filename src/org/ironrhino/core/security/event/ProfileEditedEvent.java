package org.ironrhino.core.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

public class ProfileEditedEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = 3596658507897245766L;

	public ProfileEditedEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

}
