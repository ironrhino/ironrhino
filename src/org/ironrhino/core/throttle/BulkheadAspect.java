package org.ironrhino.core.throttle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.utils.BulkheadUtils;

@Aspect
@Component
@ClassPresentConditional("io.github.resilience4j.bulkhead.Bulkhead")
public class BulkheadAspect extends BaseAspect {

	private Map<String, io.github.resilience4j.bulkhead.Bulkhead> bulkheads = new ConcurrentHashMap<>();

	public BulkheadAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(bulkhead)")
	public Object control(ProceedingJoinPoint jp, Bulkhead bulkhead) throws Throwable {
		String key = buildKey(jp);
		io.github.resilience4j.bulkhead.Bulkhead bh = bulkheads.computeIfAbsent(key, k -> {
			BulkheadConfig config = BulkheadConfig.custom().maxConcurrentCalls(bulkhead.maxConcurrentCalls())
					.maxWaitTime(bulkhead.maxWaitTime()).build();
			return io.github.resilience4j.bulkhead.Bulkhead.of(k, config);
		});
		BulkheadUtils.isCallPermitted(bh);
		try {
			return jp.proceed();
		} finally {
			bh.onComplete();
		}
	}

}
