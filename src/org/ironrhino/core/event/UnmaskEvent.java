package org.ironrhino.core.event;

import org.ironrhino.core.struts.I18N;

import lombok.Getter;

public class UnmaskEvent extends AbstractAuditEvent {

	private static final long serialVersionUID = -6090070171986100664L;

	@Getter
	private final Object principal;

	@Getter
	private final String property;

	@Getter
	private final String cleartext;

	public UnmaskEvent(String username, String remoteAddr, Object principal, String property, String cleartext) {
		super(username, remoteAddr);
		this.principal = principal;
		this.property = property;
		this.cleartext = cleartext;
	}

	@Override
	public String[] getArguments() {
		return new String[] { getUsername(), String.valueOf(principal), I18N.getText(property), cleartext };
	}

}