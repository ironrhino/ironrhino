package org.ironrhino.core.throttle;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.BulkheadConfig;

@Aspect
@Component
@ClassPresentConditional("io.github.resilience4j.bulkhead.Bulkhead")
public class BulkheadAspect extends BaseAspect {

	@Autowired
	private BulkheadRegistry bulkheadRegistry;

	public BulkheadAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @annotation(bulkhead)")
	public Object control(ProceedingJoinPoint jp, Bulkhead bulkhead) throws Throwable {
		return bulkheadRegistry.executeCheckedCallable(buildKey(jp), () -> BulkheadConfig.custom()
				.maxConcurrentCalls(bulkhead.maxConcurrentCalls()).maxWaitTime(bulkhead.maxWaitTime()).build(),
				jp::proceed);
	}

}
