package org.ironrhino.core.util;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CounterUtils {

	public static int getAndIncrement(AtomicInteger counter, int mod) {
		return increment(counter, mod, false);
	}

	public static int incrementAndGet(AtomicInteger counter, int mod) {
		return increment(counter, mod, true);
	}

	private static int increment(AtomicInteger counter, int mod, boolean incrementAndGet) {
		int current, next;
		do {
			current = counter.get();
			next = ((current != Integer.MAX_VALUE ? current : -1) + 1) % mod;
		} while (!counter.compareAndSet(current, next));
		return (next + mod + (incrementAndGet ? 0 : -1)) % mod;
	}

}
