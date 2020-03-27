package org.ironrhino.core.security.event;

import org.ironrhino.core.event.AbstractAuditEvent;

import lombok.Getter;

public abstract class TargetOrientedAuditEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -7644901666982979630L;

	@Getter
	private String targetUsername;

	public TargetOrientedAuditEvent(String username, String remoteAddr, String targetUsername) {
		super(username, remoteAddr);
		this.targetUsername = targetUsername;
	}

	public String[] getArguments() {
		return new String[] { targetUsername };
	}

}
