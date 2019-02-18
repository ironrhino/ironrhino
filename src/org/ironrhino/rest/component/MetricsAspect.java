package org.ironrhino.rest.component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.metrics.Metrics;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

@Aspect
@ControllerAdvice
public class MetricsAspect extends AbstractInstrumentAspect {

	public MetricsAspect(String servletMapping) {
		super(servletMapping);
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @within(restController)")
	public Object timing(ProceedingJoinPoint pjp, RestController restController) throws Throwable {
		if (Metrics.isMicrometerPresent())
			return pjp.proceed();
		Mapping mapping = getMapping(pjp);
		if (mapping == null)
			return pjp.proceed();
		return Metrics.recordThrowableCallable("rest.calls", pjp::proceed, "method", mapping.getMethod(), "uri",
				mapping.getUri());
	}

}
