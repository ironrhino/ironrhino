package org.ironrhino.security.event;

import org.ironrhino.core.event.BaseEvent;
import org.springframework.security.core.userdetails.UserDetails;

public class LogoutEvent extends BaseEvent<UserDetails> {

	private static final long serialVersionUID = -6090070171986100664L;

	public LogoutEvent(UserDetails user) {
		super(user);
	}

	public UserDetails getUser() {
		return getSource();
	}

}
