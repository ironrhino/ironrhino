package org.ironrhino.core.throttle;

import static io.github.resilience4j.circuitbreaker.utils.MetricNames.BUFFERED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.FAILED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.FAILURE_RATE;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.STATE;
import static io.github.resilience4j.circuitbreaker.utils.MetricNames.SUCCESSFUL;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.ironrhino.core.metrics.Metrics;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@CircuitBreakerEnabled
@Slf4j
public class CircuitBreakerRegistry {

	private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	public CircuitBreaker of(String name, Predicate<Throwable> predicate) {
		return of(name, () -> defaultBuilder().recordException(predicate).build());
	}

	public CircuitBreaker of(String name, Supplier<CircuitBreakerConfig> configSupplier) {
		return circuitBreakers.computeIfAbsent(name, key -> {
			CircuitBreaker circuitBreaker = CircuitBreaker.of(key, configSupplier.get());
			if (Metrics.isEnabled()) {
				String prefix = DEFAULT_PREFIX + '.' + key + '.';
				Metrics.gauge(prefix + STATE, circuitBreaker, cb -> cb.getState().getOrder());
				Metrics.gauge(prefix + BUFFERED, circuitBreaker, cb -> cb.getMetrics().getNumberOfBufferedCalls());
				Metrics.gauge(prefix + FAILED, circuitBreaker, cb -> cb.getMetrics().getNumberOfFailedCalls());
				Metrics.gauge(prefix + "slow", circuitBreaker, cb -> cb.getMetrics().getNumberOfSlowCalls());
				Metrics.gauge(prefix + "slow_call_rate", circuitBreaker, cb -> cb.getMetrics().getSlowCallRate());
				Metrics.gauge(prefix + FAILURE_RATE, circuitBreaker, cb -> cb.getMetrics().getFailureRate());
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

	public void transitionState(String name, String oldState, String newState) {
		transitionState(name, State.valueOf(oldState), State.valueOf(newState));
	}

	public void transitionState(String name, State oldState, State newState) {
		CircuitBreaker circuitBreaker = circuitBreakers.get(name);
		if (circuitBreaker != null) {
			synchronized (circuitBreaker) {
				if (circuitBreaker.getState() == oldState) {
					switch (newState) {
					case DISABLED:
						circuitBreaker.transitionToDisabledState();
						break;
					case CLOSED:
						circuitBreaker.transitionToClosedState();
						break;
					case OPEN:
						circuitBreaker.transitionToOpenState();
						break;
					case FORCED_OPEN:
						circuitBreaker.transitionToForcedOpenState();
						break;
					case HALF_OPEN:
						circuitBreaker.transitionToHalfOpenState();
						break;
					default:
						break;
					}
					log.info("Change state of CircuitBreaker('{}') from {} to {}", name, oldState, newState);
				} else {
					throw new OptimisticLockingFailureException("State changed, please refresh and retry.");
				}
			}
		}
	}

}
