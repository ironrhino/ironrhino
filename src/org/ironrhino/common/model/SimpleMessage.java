package org.ironrhino.common.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SimpleMessage implements Serializable {

	private static final long serialVersionUID = -4969074449395319492L;

	private String from;

	private String to;

	private String subject;

	private String content;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
