package org.ironrhino.core.security.event;

public class EditProfileEvent extends TargetOrientedAuditEvent {

	private static final long serialVersionUID = 6444717773560713552L;

	public EditProfileEvent(String username, String remoteAddr, String targetUsername) {
		super(username, remoteAddr, targetUsername);
	}

}
