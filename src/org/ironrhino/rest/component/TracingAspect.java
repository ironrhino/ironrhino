package org.ironrhino.rest.component;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

@Aspect
@ControllerAdvice
public class TracingAspect extends AbstractInstrumentAspect {

	public TracingAspect(String servletMapping) {
		super(servletMapping);
	}

	@Around("execution(public * *(..)) and @within(restController)")
	public Object trace(ProceedingJoinPoint pjp, RestController restController) throws Throwable {
		if (!Tracing.isEnabled())
			return pjp.proceed();
		Mapping mapping = getMapping(pjp);
		if (mapping == null)
			return pjp.proceed();
		return Tracing.executeCheckedCallable(
				ReflectionUtils.stringify(((MethodSignature) pjp.getSignature()).getMethod()), pjp::proceed,
				"span.kind", "server", "component", "rest", "http.method", mapping.getMethod(), "http.url",
				mapping.getUri());
	}

}
