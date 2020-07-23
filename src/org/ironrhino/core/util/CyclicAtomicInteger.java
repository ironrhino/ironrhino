package org.ironrhino.core.util;

import java.util.concurrent.atomic.AtomicInteger;

public class CyclicAtomicInteger extends Number implements java.io.Serializable {

	private static final long serialVersionUID = 8483219863927381616L;

	private final AtomicInteger counter;

	private final int offset;

	private final int bound;

	public CyclicAtomicInteger(int upperBound) {
		this(0, 0, upperBound);
	}

	public CyclicAtomicInteger(int initialValue, int lowerBound, int upperBound) {
		if (upperBound <= lowerBound)
			throw new IllegalArgumentException("upperBound must greater than lowerBound");
		if (initialValue < lowerBound || initialValue >= upperBound)
			throw new IllegalArgumentException(
					"initialValue must greater or equal than lowerBound and less than upperBound");
		this.offset = lowerBound;
		this.bound = upperBound - lowerBound;
		this.counter = new AtomicInteger(initialValue - offset);
	}

	public int get() {
		return this.counter.get() + offset;
	}

	public void set(int newValue) {
		int i = newValue - offset;
		if (i < 0 || i >= bound)
			throw new IllegalArgumentException(
					"newValue must greater or equal than lowerBound and less than upperBound");
		this.counter.set(i);
	}

	public boolean compareAndSet(int expect, int update) {
		int i = expect - offset;
		int j = update - offset;
		if (j < 0 || j >= bound)
			throw new IllegalArgumentException("update must greater or equal than lowerBound and less than upperBound");
		return this.counter.compareAndSet(i, j);
	}

	public int getAndIncrement() {
		return getAndAdd(1);
	}

	public int incrementAndGet() {
		return addAndGet(1);
	}

	public int getAndDecrement() {
		return getAndAdd(-1);
	}

	public int decrementAndGet() {
		return addAndGet(-1);
	}

	public int getAndAdd(int delta) {
		return cas(true, delta);
	}

	public int addAndGet(int delta) {
		return cas(false, delta);
	}

	private int cas(boolean getBeforeSet, int delta) {
		int i, j;
		delta %= bound;
		do {
			i = counter.get();
			j = (i + delta) % bound;
			if (j < 0)
				j += bound;
		} while (!counter.compareAndSet(i, j));
		return (getBeforeSet ? i : j) + offset;
	}

	@Override
	public int intValue() {
		return this.counter.intValue();
	}

	@Override
	public long longValue() {
		return this.counter.longValue();
	}

	@Override
	public float floatValue() {
		return this.counter.floatValue();
	}

	@Override
	public double doubleValue() {
		return this.counter.doubleValue();
	}

}
