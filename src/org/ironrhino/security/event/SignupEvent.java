package org.ironrhino.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

import lombok.Getter;

public class SignupEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	@Getter
	private String from; // oauth

	@Getter
	private String provider; // google github

	public SignupEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

	public SignupEvent(String username, String remoteAddr, String from, String provider) {
		super(username, remoteAddr);
		this.from = from;
		this.provider = provider;
	}

}
