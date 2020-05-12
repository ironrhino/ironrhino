package org.ironrhino.core.throttle;

import java.time.Duration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

@Aspect
@Component
@ClassPresentConditional("io.github.resilience4j.circuitbreaker.CircuitBreaker")
public class CircuitBreakerAspect extends BaseAspect {

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	public CircuitBreakerAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(circuitBreaker)")
	public Object control(ProceedingJoinPoint jp, CircuitBreaker circuitBreaker) throws Throwable {
		return circuitBreakerRegistry.executeCheckedCallable(buildKey(jp),
				() -> CircuitBreakerConfig.custom().failureRateThreshold(circuitBreaker.failureRateThreshold())
						.waitDurationInOpenState(Duration.ofSeconds(circuitBreaker.waitDurationInOpenState()))
						.ringBufferSizeInHalfOpenState(circuitBreaker.ringBufferSizeInHalfOpenState())
						.ringBufferSizeInClosedState(circuitBreaker.ringBufferSizeInClosedState())
						.recordFailure(
								ex -> matches(ex, circuitBreaker.include()) && !matches(ex, circuitBreaker.exclude()))
						.build(),
				jp::proceed);
	}

	private static boolean matches(Throwable failure, Class<? extends Throwable>[] types) {
		for (Class<? extends Throwable> type : types)
			if (type.isInstance(failure))
				return true;
		return false;
	}

}
