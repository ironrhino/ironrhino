package org.ironrhino.common.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SimpleMessage implements Serializable {

	private static final long serialVersionUID = -4969074449395319492L;

	private String from;

	private String to;

	private String message;

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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
