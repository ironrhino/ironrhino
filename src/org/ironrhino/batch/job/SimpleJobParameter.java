package org.ironrhino.batch.job;

import java.io.Serializable;

import org.springframework.batch.core.JobParameter.ParameterType;

public class SimpleJobParameter implements Serializable {

	private static final long serialVersionUID = 5528633144479004387L;

	private String key;

	private String value;

	private ParameterType type;

	private boolean required;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ParameterType getType() {
		return type;
	}

	public void setType(ParameterType type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

}
