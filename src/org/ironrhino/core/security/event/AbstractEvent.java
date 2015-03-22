package org.ironrhino.core.security.event;

import org.ironrhino.core.event.BaseEvent;

public abstract class AbstractEvent extends BaseEvent<String> {

	private static final long serialVersionUID = 2656926225727363987L;

	private String remoteAddr;

	private String from; // oauth

	private String provider; // google github

	public AbstractEvent(String username, String remoteAddr) {
		super(username);
		this.remoteAddr = remoteAddr;
	}

	public AbstractEvent(String username, String remoteAddr, String from,
			String provider) {
		super(username);
		this.remoteAddr = remoteAddr;
		this.from = from;
		this.provider = provider;
	}

	public String getUsername() {
		return getSource();
	}

	public String getRemoteAddr() {
		return remoteAddr;
	}

	public String getFrom() {
		return from;
	}

	public String getProvider() {
		return provider;
	}

}
