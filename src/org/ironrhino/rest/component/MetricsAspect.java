package org.ironrhino.rest.component;

import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

@Aspect
@ControllerAdvice
public class MetricsAspect extends BaseAspect {

	private final String servletMapping;

	public MetricsAspect(String servletMapping) {
		if (servletMapping.endsWith("/*"))
			servletMapping = servletMapping.substring(0, servletMapping.length() - 2);
		this.servletMapping = servletMapping;
		order = Ordered.HIGHEST_PRECEDENCE + 2;
	}

	@Around("execution(public * *(..)) and @within(restController)")
	public Object timing(ProceedingJoinPoint pjp, RestController restController) throws Throwable {
		RequestMapping requestMapping = AnnotatedElementUtils
				.findMergedAnnotation(((MethodSignature) pjp.getSignature()).getMethod(), RequestMapping.class);
		if (requestMapping == null)
			return pjp.proceed();
		RequestMapping requestMappingWithClass = AnnotatedElementUtils.findMergedAnnotation(pjp.getTarget().getClass(),
				RequestMapping.class);
		StringBuilder sb = new StringBuilder(servletMapping);
		if (requestMappingWithClass != null) {
			String[] pathWithClass = requestMappingWithClass.path();
			if (pathWithClass.length > 0)
				sb.append(pathWithClass[0]);
		}
		String[] path = requestMapping.path();
		sb.append(path.length > 0 ? path[0] : "");
		String method = requestMapping.method().length > 0 ? requestMapping.method()[0].toString() : "GET";
		return recordThrowable(Metrics.timer("rest.calls", "uri", sb.toString(), "method", method), pjp::proceed);
	}

	private static Object recordThrowable(Timer timer, ThrowableCallable f) throws Throwable {
		MeterRegistry registry = Metrics.globalRegistry;
		long start = registry.config().clock().monotonicTime();
		try {
			return f.call();
		} finally {
			timer.record(registry.config().clock().monotonicTime() - start, TimeUnit.NANOSECONDS);
		}
	}

}
