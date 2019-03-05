package org.ironrhino.core.throttle;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

@Aspect
@Component
@ClassPresentConditional("io.github.resilience4j.ratelimiter.RateLimiter")
public class RateLimiterAspect extends BaseAspect {

	private ConcurrentHashMap<String, io.github.resilience4j.ratelimiter.RateLimiter> rateLimiters = new ConcurrentHashMap<>();

	public RateLimiterAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(rateLimiter)")
	public Object control(ProceedingJoinPoint jp, RateLimiter rateLimiter) throws Throwable {
		String key = jp.getSignature().toLongString();
		io.github.resilience4j.ratelimiter.RateLimiter limiter = rateLimiters.computeIfAbsent(key, k -> {
			RateLimiterConfig config = RateLimiterConfig.custom()
					.timeoutDuration(Duration.ofMillis(rateLimiter.timeoutDuration()))
					.limitRefreshPeriod(Duration.ofMillis(rateLimiter.limitRefreshPeriod()))
					.limitForPeriod(rateLimiter.limitForPeriod()).build();
			return io.github.resilience4j.ratelimiter.RateLimiter.of(k, config);
		});
		RateLimiterConfig rateLimiterConfig = limiter.getRateLimiterConfig();
		Duration timeoutDuration = rateLimiterConfig.getTimeoutDuration();
		boolean permission = limiter.getPermission(timeoutDuration);
		if (Thread.interrupted()) {
			throw new IllegalStateException("Thread was interrupted during permission wait");
		}
		if (!permission) {
			throw new RequestNotPermitted("Request not permitted for limiter: " + limiter.getName());
		}
		return jp.proceed();
	}

}
