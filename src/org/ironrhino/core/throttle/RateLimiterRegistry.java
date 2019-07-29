package org.ironrhino.core.throttle;

import static io.github.resilience4j.ratelimiter.utils.MetricNames.AVAILABLE_PERMISSIONS;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.WAITING_THREADS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.ironrhino.core.metrics.Metrics;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.Getter;

@Component
@Getter
@ClassPresentConditional("io.github.resilience4j.ratelimiter.RateLimiter")
public class RateLimiterRegistry {

	private final Map<String, io.github.resilience4j.ratelimiter.RateLimiter> rateLimiters = new ConcurrentHashMap<>();

	public <T, E extends Throwable> T executeThrowableCallable(String name, Supplier<RateLimiterConfig> configSupplier,
			ThrowableCallable<T, E> callable) throws E {
		io.github.resilience4j.ratelimiter.RateLimiter limiter = of(name, configSupplier);
		RateLimiterConfig rateLimiterConfig = limiter.getRateLimiterConfig();
		boolean permission = limiter.getPermission(rateLimiterConfig.getTimeoutDuration());
		if (Thread.interrupted()) {
			throw new IllegalStateException("Thread was interrupted during permission wait");
		}
		if (!permission) {
			throw new RequestNotPermitted("Request not permitted for limiter: " + limiter.getName());
		}
		return callable.call();
	}

	public io.github.resilience4j.ratelimiter.RateLimiter of(String name, Supplier<RateLimiterConfig> configSupplier) {
		return getRateLimiters().computeIfAbsent(name, key -> {
			io.github.resilience4j.ratelimiter.RateLimiter limiter = io.github.resilience4j.ratelimiter.RateLimiter
					.of(key, configSupplier.get());
			if (Metrics.isEnabled()) {
				String prefix = DEFAULT_PREFIX + '.' + key + '.';
				Metrics.gauge(prefix + WAITING_THREADS, limiter, rl -> rl.getMetrics().getNumberOfWaitingThreads());
				Metrics.gauge(prefix + AVAILABLE_PERMISSIONS, limiter, rl -> rl.getMetrics().getAvailablePermissions());
			}
			return limiter;
		});
	}

}
