package org.ironrhino.core.util;

import java.util.concurrent.atomic.AtomicLong;

public class Sequence {

	public final long max;

	private AtomicLong seed = new AtomicLong();

	public Sequence() {
		this.max = Long.MAX_VALUE;
	}

	public Sequence(long max) {
		this.max = max;
	}

	public long next() {
		long val = seed.incrementAndGet();
		if (val > max) {
			while (true)
				if (seed.compareAndSet(val, 0))
					break;
			val = seed.incrementAndGet();
		}
		return val;
	}
}
