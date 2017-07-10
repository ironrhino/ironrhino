package org.ironrhino.core.scheduled;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.NameGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ScheduledTaskCircuitBreakerAspect extends BaseAspect {

	@Autowired(required = false)
	private ScheduledTaskCircuitBreaker circuitBreaker;

	public ScheduledTaskCircuitBreakerAspect() {
		order = -10000;
	}

	@Around("execution(public * *(..)) and @annotation(scheduled)")
	public Object control(ProceedingJoinPoint jp, Scheduled scheduled) throws Throwable {
		if (circuitBreaker == null)
			return jp.proceed();
		StringBuilder sb = new StringBuilder();
		Class<?> beanClass = jp.getTarget().getClass();
		String beanName = NameGenerator.buildDefaultBeanName(beanClass.getName());
		Component comp = AnnotatedElementUtils.getMergedAnnotation(beanClass, Component.class);
		if (StringUtils.isNotBlank(comp.value()))
			beanName = comp.value();
		String task = sb.append(beanName).append('.').append(jp.getSignature().getName()).append('(').append(')')
				.toString();
		if (circuitBreaker.isShortCircuit(task)) {
			throw new IllegalStateException("Execution[\"" + task + "\"] is short circuit");
		} else {
			return jp.proceed();
		}
	}

}
