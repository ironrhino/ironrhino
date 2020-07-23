package org.ironrhino.core.util;

import java.util.concurrent.atomic.AtomicInteger;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CounterUtils {

	public static int getAndIncrement(AtomicInteger counter, int mod) {
		int value = incrementAndGet(counter, mod) - 1;
		return value < 0 ? value + mod : value;
	}

	public static int incrementAndGet(AtomicInteger counter, int mod) {
		int current, next;
		do {
			current = counter.get();
			next = ((current != Integer.MAX_VALUE ? current : -1) + 1) % mod;
			if (next < 0)
				next += mod;
		} while (!counter.compareAndSet(current, next));
		return next;
	}

}
