package org.ironrhino.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

public class LoginEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	private boolean first;

	private String from; // oauth

	private String provider; // google github

	public LoginEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

	public LoginEvent(String username, String remoteAddr, String from, String provider) {
		super(username, remoteAddr);
		this.from = from;
		this.provider = provider;
	}

	public boolean isFirst() {
		return first;
	}

	public void setFirst(boolean first) {
		this.first = first;
	}

	public String getFrom() {
		return from;
	}

	public String getProvider() {
		return provider;
	}

}
