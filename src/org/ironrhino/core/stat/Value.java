package org.ironrhino.core.stat;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

public class Value implements Serializable {

	private static final long serialVersionUID = 3432914727431442150L;

	private final AtomicLong longValue;

	private final AtomicLong doubleValue;

	public static final int PRECISION = 1000;

	// private double doubleValue;

	public Value() {
		longValue = new AtomicLong(0);
		doubleValue = new AtomicLong(0);
	}

	public Value(long c) {
		longValue = new AtomicLong(c);
		doubleValue = new AtomicLong(0);
	}

	public Value(double d) {
		longValue = new AtomicLong(0);
		doubleValue = new AtomicLong((long) (d * PRECISION));
	}

	public Value(long c, double d) {
		longValue = new AtomicLong(c);
		doubleValue = new AtomicLong((long) (d * PRECISION));
	}

	public Number[] add(long c, double d) {
		c = addLongValue(c);
		d = addDoubleValue(d);
		return new Number[] { c, d };
	}

	public long addLongValue(long value) {
		return longValue.addAndGet(value);
	}

	public long getLongValue() {
		return longValue.get();
	}

	public double addDoubleValue(double value) {
		return ((double) doubleValue.getAndAdd((long) (value * PRECISION))) / PRECISION;
	}

	public double getDoubleValue() {
		return ((double) doubleValue.get()) / PRECISION;
	}

	@Override
	public String toString() {
		return String.valueOf(getLongValue()) + "," + String.valueOf(getDoubleValue());
	}

	public static Value fromString(String s) {
		if (StringUtils.isBlank(s))
			return null;
		String[] array = s.split(",");
		return new Value(Long.valueOf(array[0]), Double.valueOf(array[1]));
	}

	public void cumulate(Value value) {
		this.addLongValue(value.getLongValue());
		this.addDoubleValue(value.getDoubleValue());
	}

}
