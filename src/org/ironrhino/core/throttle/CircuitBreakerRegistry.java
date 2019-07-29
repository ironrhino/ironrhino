package org.ironrhino.core.throttle;

import static io.github.resilience4j.circuitbreaker.utils.MetricNames.BUFFERED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.BUFFERED_MAX;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.FAILED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.STATE;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.SUCCESSFUL;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;
import lombok.Getter;

@Component
@Getter
@ClassPresentConditional("io.github.resilience4j.circuitbreaker.CircuitBreaker")
public class CircuitBreakerRegistry {

	private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	public <T> T execute(String name, Predicate<Throwable> predicate, Callable<T> callable) throws Exception {
		return of(name, predicate).executeCallable(callable);
	}

	public <T, E extends Throwable> T executeThrowableCallable(String name, Predicate<Throwable> predicate,
			ThrowableCallable<T, E> callable) throws E {
		return executeThrowableCallable(of(name, predicate), callable);
	}

	public <T, E extends Throwable> T executeThrowableCallable(String name,
			Supplier<CircuitBreakerConfig> configSupplier, ThrowableCallable<T, E> callable) throws E {
		return executeThrowableCallable(of(name, configSupplier), callable);
	}

	<T, E extends Throwable> T executeThrowableCallable(CircuitBreaker circuitBreaker, ThrowableCallable<T, E> callable)
			throws E {
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

	public CircuitBreaker of(String name, Predicate<Throwable> predicate) {
		return of(name, () -> defaultBuilder().recordFailure(predicate).build());
	}

	public CircuitBreaker of(String name, Supplier<CircuitBreakerConfig> configSupplier) {
		return getCircuitBreakers().computeIfAbsent(name, key -> {
			CircuitBreaker circuitBreaker = CircuitBreaker.of(key, configSupplier.get());
			if (Metrics.isEnabled()) {
				String prefix = DEFAULT_PREFIX + '.' + key + '.';
				Metrics.gauge(prefix + STATE, circuitBreaker, cb -> cb.getState().getOrder());
				Metrics.gauge(prefix + BUFFERED_MAX, circuitBreaker,
						cb -> cb.getMetrics().getMaxNumberOfBufferedCalls());
				Metrics.gauge(prefix + BUFFERED, circuitBreaker, cb -> cb.getMetrics().getNumberOfBufferedCalls());
				Metrics.gauge(prefix + FAILED, circuitBreaker, cb -> cb.getMetrics().getNumberOfFailedCalls());
				Metrics.gauge(prefix + NOT_PERMITTED, circuitBreaker,
						cb -> cb.getMetrics().getNumberOfNotPermittedCalls());
				Metrics.gauge(prefix + SUCCESSFUL, circuitBreaker, cb -> cb.getMetrics().getNumberOfSuccessfulCalls());
			}
			return circuitBreaker;
		});
	}

	public CircuitBreakerConfig.Builder defaultBuilder() {
		return CircuitBreakerConfig.custom().failureRateThreshold(95);
	}

}
