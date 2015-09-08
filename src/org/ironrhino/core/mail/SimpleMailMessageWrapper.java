package org.ironrhino.core.mail;

import java.io.Serializable;

import org.springframework.mail.SimpleMailMessage;

public class SimpleMailMessageWrapper implements Serializable {

	private static final long serialVersionUID = 1654838401432724325L;

	private SimpleMailMessage simpleMailMessage;

	private boolean useHtmlFormat = true;

	public SimpleMailMessageWrapper() {
	}

	public SimpleMailMessageWrapper(SimpleMailMessage simpleMailMessage) {
		this.simpleMailMessage = simpleMailMessage;
	}

	public SimpleMailMessageWrapper(SimpleMailMessage simpleMailMessage, boolean useHtmlFormat) {
		this.simpleMailMessage = simpleMailMessage;
		this.useHtmlFormat = useHtmlFormat;
	}

	public SimpleMailMessage getSimpleMailMessage() {
		return simpleMailMessage;
	}

	public void setSimpleMailMessage(SimpleMailMessage simpleMailMessage) {
		this.simpleMailMessage = simpleMailMessage;
	}

	public boolean isUseHtmlFormat() {
		return useHtmlFormat;
	}

	public void setUseHtmlFormat(boolean useHtmlFormat) {
		this.useHtmlFormat = useHtmlFormat;
	}

}
