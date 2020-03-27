package org.ironrhino.core.security.event;

public class RemovePasswordEvent extends TargetOrientedAuditEvent {

	private static final long serialVersionUID = -3312183500304444942L;

	public RemovePasswordEvent(String username, String remoteAddr, String targetUsername) {
		super(username, remoteAddr, targetUsername);
	}

}
