package org.ironrhino.core.security.event;

public class ResetPasswordEvent extends TargetOrientedAuditEvent {

	private static final long serialVersionUID = -373603607106874368L;

	public ResetPasswordEvent(String username, String remoteAddr, String targetUsername) {
		super(username, remoteAddr, targetUsername);
	}

}
