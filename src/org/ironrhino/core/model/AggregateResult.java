package org.ironrhino.core.model;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

@Data
public class AggregateResult implements Serializable {

	private static final long serialVersionUID = -4526897485932692316L;

	private Object principal;

	private Number average;

	private Number count;

	private Number sum;

	private Number max;

	private Number min;

	private Map<Number, Number> details;

}
