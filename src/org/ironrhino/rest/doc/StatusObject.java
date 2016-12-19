package org.ironrhino.rest.doc;

import java.io.Serializable;

import org.ironrhino.rest.doc.annotation.Status;
import org.springframework.http.HttpStatus;

public class StatusObject implements Serializable {

	private static final long serialVersionUID = 7945088092832154760L;
	private int code;
	private String message;
	private String description;

	public StatusObject() {

	}

	public StatusObject(int code, String message, String description) {
		this.code = code;
		this.message = message;
		this.description = description;
	}

	public StatusObject(Status status) {
		this.code = status.code();
		this.message = status.message();
		this.description = status.description();
	}

	public StatusObject(HttpStatus httpStatus) {
		this.code = httpStatus.value();
		this.message = httpStatus.getReasonPhrase();
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
