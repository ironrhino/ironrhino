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

import io.github.resilience4j.ratelimiter.RateLimiterConfig;

@Aspect
@Component
@ClassPresentConditional("io.github.resilience4j.ratelimiter.RateLimiter")
public class RateLimiterAspect extends BaseAspect {

	@Autowired
	private RateLimiterRegistry rateLimiterRegistry;

	public RateLimiterAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(rateLimiter)")
	public Object control(ProceedingJoinPoint jp, RateLimiter rateLimiter) throws Throwable {
		return rateLimiterRegistry.executeThrowableCallable(buildKey(jp),
				() -> RateLimiterConfig.custom().timeoutDuration(Duration.ofMillis(rateLimiter.timeoutDuration()))
						.limitRefreshPeriod(Duration.ofMillis(rateLimiter.limitRefreshPeriod()))
						.limitForPeriod(rateLimiter.limitForPeriod()).build(),
				jp::proceed);
	}

}
