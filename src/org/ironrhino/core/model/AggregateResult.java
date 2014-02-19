package org.ironrhino.core.model;

import java.io.Serializable;
import java.util.Map;

public class AggregateResult implements Serializable {

	private static final long serialVersionUID = -4526897485932692316L;

	private Object principal;

	private Number average;

	private Number count;

	private Number sum;

	private Number max;

	private Number min;

	private Map<Number, Number> details;

	public Number getAverage() {
		return average;
	}

	public void setAverage(Number average) {
		this.average = average;
	}

	public Number getCount() {
		return count;
	}

	public void setCount(Number count) {
		this.count = count;
	}

	public Number getMax() {
		return max;
	}

	public void setMax(Number max) {
		this.max = max;
	}

	public Number getMin() {
		return min;
	}

	public void setMin(Number min) {
		this.min = min;
	}

	public Object getPrincipal() {
		return principal;
	}

	public void setPrincipal(Object principal) {
		this.principal = principal;
	}

	public Number getSum() {
		return sum;
	}

	public void setSum(Number sum) {
		this.sum = sum;
	}

	public Map<Number, Number> getDetails() {
		return details;
	}

	public void setDetails(Map<Number, Number> details) {
		this.details = details;
	}

}
