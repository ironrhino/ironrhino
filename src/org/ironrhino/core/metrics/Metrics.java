package org.ironrhino.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.ToDoubleFunction;

import org.ironrhino.core.util.ThrowableCallable;
import org.ironrhino.core.util.ThrowableRunnable;
import org.springframework.util.ClassUtils;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Metrics {

	private static final boolean micrometerPresent = ClassUtils.isPresent("io.micrometer.core.instrument.Metrics",
			Metrics.class.getClassLoader());

	public static boolean isMicrometerPresent() {
		return micrometerPresent;
	}

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

	public static void recordTimer(String name, Runnable runnable, String... tags) {
		if (!micrometerPresent) {
			runnable.run();
			return;
		}
		io.micrometer.core.instrument.Metrics.timer(name, tags).record(runnable);
	}

	public static <T, E extends Throwable> T recordThrowableCallable(String name, ThrowableCallable<T, E> callable,
			String... tags) throws E {
		if (!micrometerPresent)
			return callable.call();
		Timer timer = io.micrometer.core.instrument.Metrics.timer(name, tags);
		long start = System.nanoTime();
		try {
			return callable.call();
		} finally {
			timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}

	public static <E extends Throwable> void recordThrowableRunnable(String name, ThrowableRunnable<E> runnable,
			String... tags) throws E {
		if (!micrometerPresent) {
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

	public static <T> T gauge(String name, T obj, ToDoubleFunction<T> valueFunction, String... tags) {
		if (!micrometerPresent)
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
