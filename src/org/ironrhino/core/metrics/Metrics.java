package org.ironrhino.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

import org.ironrhino.core.util.CheckedCallable;
import org.ironrhino.core.util.CheckedRunnable;
import org.springframework.util.ClassUtils;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Metrics {

	private static volatile boolean enabled = ClassUtils.isPresent("io.micrometer.core.instrument.Metrics",
			Metrics.class.getClassLoader());

	static void disable() {
		enabled = false;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void recordTimer(String name, long amount, TimeUnit unit, String... tags) {
		if (!enabled)
			return;
		io.micrometer.core.instrument.Metrics.timer(name, tags).record(amount, unit);
	}

	public static <T> T recordTimer(String name, Callable<T> callable, String... tags) throws Exception {
		if (!enabled)
			return callable.call();
		return io.micrometer.core.instrument.Metrics.timer(name, tags).recordCallable(callable);
	}

	public static void recordTimer(String name, Runnable runnable, String... tags) {
		if (!enabled) {
			runnable.run();
			return;
		}
		io.micrometer.core.instrument.Metrics.timer(name, tags).record(runnable);
	}

	public static <T, E extends Throwable> T recordCheckedCallable(String name, CheckedCallable<T, E> callable,
			String... tags) throws E {
		if (!enabled)
			return callable.call();
		Timer timer = io.micrometer.core.instrument.Metrics.timer(name, tags);
		long start = System.nanoTime();
		try {
			return callable.call();
		} finally {
			timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	public static <E extends Throwable> void recordCheckedRunnable(String name, CheckedRunnable<E> runnable,
			String... tags) throws E {
		if (!enabled) {
			runnable.run();
			return;
		}
		Timer timer = io.micrometer.core.instrument.Metrics.timer(name, tags);
		long start = System.nanoTime();
		try {
			runnable.run();
		} finally {
			timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	public static void recordSummary(String name, long amount, String... tags) {
		if (!enabled)
			return;
		io.micrometer.core.instrument.Metrics.summary(name, tags).record(amount);
	}

	public static void increment(String name, String... tags) {
		if (!enabled)
			return;
		io.micrometer.core.instrument.Metrics.counter(name, tags).increment();
	}

	public static void increment(String name, double amount, String... tags) {
		if (!enabled)
			return;
		io.micrometer.core.instrument.Metrics.counter(name, tags).increment(amount);
	}

	public static <T> T gauge(String name, T obj, ToDoubleFunction<T> valueFunction, String... tags) {
		if (!enabled)
			return obj;
		if (tags.length == 0) {
			return io.micrometer.core.instrument.Metrics.gauge(name, obj, valueFunction);
		} else {
			List<Tag> list = new ArrayList<>();
			for (int i = 0; i < tags.length; i += 2)
				list.add(Tag.of(tags[i], tags[i + 1]));
			return io.micrometer.core.instrument.Metrics.gauge(name, list, obj, valueFunction);
		}
	}

}
