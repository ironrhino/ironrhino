package org.ironrhino.core.metrics;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.util.ClassUtils;

public class Metrics {

	private static boolean micrometerPresent = ClassUtils.isPresent("io.micrometer.core.instrument.Metrics",
			Metrics.class.getClassLoader());

	public static void recordTimer(String name, long amount, TimeUnit unit, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.timer(name, tags).record(amount, unit);
	}

	public static <T> T recordTimer(String name, Callable<T> callable, String... tags) throws Exception {
		if (!micrometerPresent)
			return callable.call();
		return io.micrometer.core.instrument.Metrics.timer(name, tags).recordCallable(callable);
	}

	public static <T> T recordThrowableCallable(String name, ThrowableCallable<T> callable, String... tags)
			throws Throwable {
		if (!micrometerPresent)
			return callable.call();
		io.micrometer.core.instrument.MeterRegistry registry = io.micrometer.core.instrument.Metrics.globalRegistry;
		io.micrometer.core.instrument.Timer timer = io.micrometer.core.instrument.Metrics.timer(name, tags);
		long start = registry.config().clock().monotonicTime();
		try {
			return callable.call();
		} finally {
			timer.record(registry.config().clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	public static void recordSummary(String name, long amount, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.summary(name, tags).record(amount);
	}

	public static void increment(String name, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.counter(name, tags).increment();
	}

	public static void increment(String name, double amount, String... tags) {
		if (!micrometerPresent)
			return;
		io.micrometer.core.instrument.Metrics.counter(name, tags).increment(amount);
	}

}
