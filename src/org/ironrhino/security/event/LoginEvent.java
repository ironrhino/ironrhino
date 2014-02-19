package org.ironrhino.security.event;

import org.springframework.security.core.userdetails.UserDetails;

public class LoginEvent extends AbstractEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	private boolean first;

	public LoginEvent(UserDetails user) {
		super(user);
	}

	public LoginEvent(UserDetails user, String from, String provider) {
		super(user, from, provider);
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

}
