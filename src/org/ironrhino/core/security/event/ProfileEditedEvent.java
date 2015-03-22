package org.ironrhino.core.security.event;

public class ProfileEditedEvent extends AbstractEvent {

	private static final long serialVersionUID = 3596658507897245766L;

	public ProfileEditedEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

}
