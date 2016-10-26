package org.ironrhino.core.stat;

import java.io.Serializable;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.lang3.StringUtils;

public class Value implements Serializable {

	private static final long serialVersionUID = 3432914727431442150L;

	private final LongAdder longValue;

	private final LongAdder doubleValue;

	public static final int PRECISION = 1000;

	// private double doubleValue;

	public Value() {
		longValue = new LongAdder();
		doubleValue = new LongAdder();
	}

	public Value(long c) {
		longValue = new LongAdder();
		longValue.add(c);
		doubleValue = new LongAdder();
	}

	public Value(double d) {
		longValue = new LongAdder();
		doubleValue = new LongAdder();
		doubleValue.add((long) (d * PRECISION));
	}

	public Value(long c, double d) {
		longValue = new LongAdder();
		longValue.add(c);
		doubleValue = new LongAdder();
		doubleValue.add((long) (d * PRECISION));
	}

	public void add(long c, double d) {
		addLongValue(c);
		addDoubleValue(d);
	}

	public void addLongValue(long value) {
		longValue.add(value);
	}

	public long getLongValue() {
		return longValue.longValue();
	}

	public void addDoubleValue(double value) {
		doubleValue.add((long) (value * PRECISION));
	}

	public double getDoubleValue() {
		return ((double) doubleValue.longValue()) / PRECISION;
	}

	@Override
	public String toString() {
		return String.valueOf(getLongValue()) + ',' + String.valueOf(getDoubleValue());
	}

	public static Value fromString(String s) {
		if (StringUtils.isBlank(s))
			return null;
		String[] array = s.split(",");
		return new Value(Long.valueOf(array[0]), Double.valueOf(array[1]));
	}

	public void accumulate(Value value) {
		this.addLongValue(value.getLongValue());
		this.addDoubleValue(value.getDoubleValue());
	}

}
