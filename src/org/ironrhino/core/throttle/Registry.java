package org.ironrhino.core.throttle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.Getter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Registry {

	@Getter
	private static final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	@Getter
	private static final Map<String, Bulkhead> bulkheads = new ConcurrentHashMap<>();

	@Getter
	private static final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

}
