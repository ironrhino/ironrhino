package org.ironrhino.security.event;

import org.springframework.security.core.userdetails.UserDetails;

public class SignupEvent extends AbstractEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	public SignupEvent(UserDetails user) {
		super(user);
	}

	public SignupEvent(UserDetails user, String from, String provider) {
		super(user, from, provider);
	}

}
