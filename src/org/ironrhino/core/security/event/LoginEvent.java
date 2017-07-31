package org.ironrhino.core.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

import lombok.Getter;
import lombok.Setter;

public class LoginEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	@Getter
	@Setter
	private boolean first;

	@Getter
	private String from; // oauth

	@Getter
	private String provider; // google github

	public LoginEvent(String username, String remoteAddr) {
		super(username, remoteAddr);
	}

	public LoginEvent(String username, String remoteAddr, String from, String provider) {
		super(username, remoteAddr);
		this.from = from;
		this.provider = provider;
	}

}
