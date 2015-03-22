package org.ironrhino.core.security.event;

public class LoginEvent extends AbstractEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	private boolean first;

	public LoginEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

	public LoginEvent(String username, String remoteAddr, String from,
			String provider) {
		super(username, remoteAddr, from, provider);
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

}
