package org.ironrhino.core.throttle;

import static io.github.resilience4j.circuitbreaker.utils.MetricNames.BUFFERED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.BUFFERED_MAX;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.FAILED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.STATE;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.SUCCESSFUL;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.util.ThrowableCallable;
import org.ironrhino.core.util.ThrowableRunnable;
import org.springframework.util.ClassUtils;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;

public class CircuitBreaking {

	private static final boolean resilience4jPresent = ClassUtils
			.isPresent("io.github.resilience4j.circuitbreaker.CircuitBreaker", CircuitBreaking.class.getClassLoader());

	private static Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	public static boolean isResilience4jPresent() {
		return resilience4jPresent;
	}

	public static <T> T execute(String name, Predicate<Throwable> predicate, Callable<T> callable) throws Exception {
		if (!resilience4jPresent)
			return callable.call();
		return of(name, predicate).executeCallable(callable);
	}

	public static void execute(String name, Predicate<Throwable> predicate, Runnable runnable, String... tags) {
		if (!resilience4jPresent) {
			runnable.run();
			return;
		}
		of(name, predicate).executeRunnable(runnable);
	}

	public static <T, E extends Throwable> T executeThrowableCallable(String name, Predicate<Throwable> predicate,
			ThrowableCallable<T, E> callable) throws E {
		if (!resilience4jPresent)
			return callable.call();
		CircuitBreaker circuitBreaker = of(name, predicate);
		CircuitBreakerUtils.isCallPermitted(circuitBreaker);
		long start = System.nanoTime();
		try {
			T value = callable.call();
			long durationInNanos = System.nanoTime() - start;
			circuitBreaker.onSuccess(durationInNanos);
			return value;
		} catch (Throwable throwable) {
			long durationInNanos = System.nanoTime() - start;
			circuitBreaker.onError(durationInNanos, throwable);
			throw throwable;
		}
	}

	public static <E extends Throwable> void executeThrowableRunnable(String name, Predicate<Throwable> predicate,
			ThrowableRunnable<E> runnable) throws E {
		if (!resilience4jPresent) {
			runnable.run();
			return;
		}
		CircuitBreaker circuitBreaker = of(name, predicate);
		CircuitBreakerUtils.isCallPermitted(circuitBreaker);
		long start = System.nanoTime();
		try {
			runnable.run();
			long durationInNanos = System.nanoTime() - start;
			circuitBreaker.onSuccess(durationInNanos);
		} catch (Throwable throwable) {
			long durationInNanos = System.nanoTime() - start;
			circuitBreaker.onError(durationInNanos, throwable);
			throw throwable;
		}
	}

	private static CircuitBreaker of(String key, Predicate<Throwable> predicate) {
		return circuitBreakers.computeIfAbsent(key, name -> {
			CircuitBreaker circuitBreaker = CircuitBreaker.of(name,
					CircuitBreakerConfig.custom().recordFailure(predicate).build());
			if (Metrics.isMicrometerPresent()) {
				String prefix = "circuitbreaker.";
				Metrics.gauge(prefix + name + '.' + STATE, circuitBreaker, cb -> cb.getState().getOrder());
				Metrics.gauge(prefix + name + '.' + BUFFERED_MAX, circuitBreaker,
						cb -> cb.getMetrics().getMaxNumberOfBufferedCalls());
				Metrics.gauge(prefix + name + '.' + BUFFERED, circuitBreaker,
						cb -> cb.getMetrics().getNumberOfBufferedCalls());
				Metrics.gauge(prefix + name + '.' + FAILED, circuitBreaker,
						cb -> cb.getMetrics().getNumberOfFailedCalls());
				Metrics.gauge(prefix + name + '.' + NOT_PERMITTED, circuitBreaker,
						cb -> cb.getMetrics().getNumberOfNotPermittedCalls());
				Metrics.gauge(prefix + name + '.' + SUCCESSFUL, circuitBreaker,
						cb -> cb.getMetrics().getNumberOfSuccessfulCalls());
			}
			return circuitBreaker;
		});
	}

}
