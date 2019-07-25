package org.ironrhino.core.throttle;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils;

@Aspect
@Component
@ClassPresentConditional("io.github.resilience4j.circuitbreaker.CircuitBreaker")
public class CircuitBreakerAspect extends BaseAspect {

	private Map<String, io.github.resilience4j.circuitbreaker.CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

	public CircuitBreakerAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(circuitBreaker)")
	public Object control(ProceedingJoinPoint jp, CircuitBreaker circuitBreaker) throws Throwable {
		String key = buildKey(jp);
		io.github.resilience4j.circuitbreaker.CircuitBreaker cb = circuitBreakers.computeIfAbsent(key, k -> {
			CircuitBreakerConfig config = CircuitBreakerConfig.custom()
					.failureRateThreshold(circuitBreaker.failureRateThreshold())
					.waitDurationInOpenState(Duration.ofSeconds(circuitBreaker.waitDurationInOpenState()))
					.ringBufferSizeInHalfOpenState(circuitBreaker.ringBufferSizeInHalfOpenState())
					.ringBufferSizeInClosedState(circuitBreaker.ringBufferSizeInClosedState())
					.recordFailure(
							ex -> matches(ex, circuitBreaker.include()) && !matches(ex, circuitBreaker.exclude()))
					.build();
			return io.github.resilience4j.circuitbreaker.CircuitBreaker.of(k, config);
		});
		CircuitBreakerUtils.isCallPermitted(cb);
		long start = System.nanoTime();
		try {
			Object returnValue = jp.proceed();
			cb.onSuccess(System.nanoTime() - start);
			return returnValue;
		} catch (Throwable throwable) {
			cb.onError(System.nanoTime() - start, throwable);
			throw throwable;
		}
	}

	private static boolean matches(Throwable failure, Class<? extends Throwable>[] types) {
		for (Class<? extends Throwable> type : types)
			if (type.isAssignableFrom(failure.getClass()))
				return true;
		return false;
	}

}
