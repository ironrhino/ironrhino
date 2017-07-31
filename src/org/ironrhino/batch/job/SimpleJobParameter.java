package org.ironrhino.batch.job;

import java.io.Serializable;

import org.springframework.batch.core.JobParameter.ParameterType;

import lombok.Data;

@Data
public class SimpleJobParameter implements Serializable {

	private static final long serialVersionUID = 5528633144479004387L;

	private String key;

	private String value;

	private ParameterType type;

	private boolean required;

}
