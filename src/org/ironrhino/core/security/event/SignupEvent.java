package org.ironrhino.core.security.event;

public class SignupEvent extends AbstractEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	public SignupEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

	public SignupEvent(String username, String remoteAddr, String from,
			String provider) {
		super(username, remoteAddr, from, provider);
	}

}
