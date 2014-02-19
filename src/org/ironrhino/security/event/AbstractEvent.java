package org.ironrhino.security.event;

import org.ironrhino.core.event.BaseEvent;
import org.springframework.security.core.userdetails.UserDetails;

public abstract class AbstractEvent extends BaseEvent<UserDetails> {

	private static final long serialVersionUID = 2656926225727363987L;

	private String from; // oauth

	private String provider; // google github

	public AbstractEvent(UserDetails user) {
		super(user);
	}

	public AbstractEvent(UserDetails user, String from, String provider) {
		super(user);
		this.from = from;
		this.provider = provider;
	}

	public UserDetails getUser() {
		return getSource();
	}

	public String getFrom() {
		return from;
	}

	public String getProvider() {
		return provider;
	}

}
