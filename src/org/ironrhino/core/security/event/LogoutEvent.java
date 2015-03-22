package org.ironrhino.core.security.event;

public class LogoutEvent extends AbstractEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	public LogoutEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

}
