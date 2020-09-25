package org.ironrhino.core.throttle;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@Aspect
@Component
@CircuitBreakerEnabled
public class CircuitBreakerAspect extends BaseAspect {

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	public CircuitBreakerAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(circuitBreaker)")
	public Object control(ProceedingJoinPoint jp, CircuitBreaker circuitBreaker) throws Throwable {
		return circuitBreakerRegistry.of(buildKey(jp),
				() -> CircuitBreakerConfig.custom().failureRateThreshold(circuitBreaker.failureRateThreshold())
						.slowCallRateThreshold(circuitBreaker.slowCallRateThreshold())
						.slowCallDurationThreshold(Duration.ofSeconds(circuitBreaker.slowCallDurationThreshold()))
						.waitDurationInOpenState(Duration.ofSeconds(circuitBreaker.waitDurationInOpenState()))
						.permittedNumberOfCallsInHalfOpenState(circuitBreaker.permittedNumberOfCallsInHalfOpenState())
						.minimumNumberOfCalls(circuitBreaker.minimumNumberOfCalls())
						.slidingWindowSize(circuitBreaker.slidingWindowSize())
						.recordException(
								ex -> matches(ex, circuitBreaker.include()) && !matches(ex, circuitBreaker.exclude()))
						.build())
				.executeCheckedSupplier(jp::proceed);
	}

	private static boolean matches(Throwable failure, Class<? extends Throwable>[] types) {
		for (Class<? extends Throwable> type : types)
			if (type.isInstance(failure))
				return true;
		return false;
	}

}
