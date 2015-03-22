package org.ironrhino.core.security.event;

public class PasswordChangedEvent extends AbstractEvent {

	private static final long serialVersionUID = 7854178610021098541L;

	public PasswordChangedEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

}
