package org.ironrhino.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

public class SignupEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	private String from; // oauth

	private String provider; // google github

	public SignupEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

	public SignupEvent(String username, String remoteAddr, String from, String provider) {
		super(username, remoteAddr);
		this.from = from;
		this.provider = provider;
	}

	public String getFrom() {
		return from;
	}

	public String getProvider() {
		return provider;
	}

}
