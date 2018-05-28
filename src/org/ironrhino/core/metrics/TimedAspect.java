package org.ironrhino.core.metrics;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;

@Aspect
public class TimedAspect {

	@Around("execution(* *.*(..)) and @annotation(timed)")
	public Object timing(ProceedingJoinPoint pjp, Timed timed) throws Throwable {
		MeterRegistry registry = Metrics.globalRegistry;
		String name = timed.value();
		if (name.isEmpty()) {
			StringBuilder sb = new StringBuilder("timed.");
			Class<?> beanClass = pjp.getTarget().getClass();
			String beanName = NameGenerator.buildDefaultBeanName(beanClass.getName());
			Component comp = AnnotatedElementUtils.getMergedAnnotation(beanClass, Component.class);
			if (comp != null && StringUtils.isNotBlank(comp.value()))
				beanName = comp.value();
			sb.append(beanName).append('.').append(pjp.getSignature().getName()).append("()");
			name = sb.toString();
		}
		if (timed.longTask()) {
			LongTaskTimer longTaskTimer = LongTaskTimer.builder(name).tags(timed.extraTags()).register(registry);
			return recordThrowable(longTaskTimer, () -> recordThrowable(longTaskTimer, pjp::proceed));
		} else {
			Timer.Builder timerBuilder = Timer.builder(name).tags(timed.extraTags());
			if (timed.histogram())
				timerBuilder.publishPercentileHistogram();
			if (timed.percentiles().length > 0)
				timerBuilder = timerBuilder.publishPercentiles(timed.percentiles());
			Timer timer = timerBuilder.register(registry);
			return recordThrowable(timer, pjp::proceed);
		}
	}

	@Around("execution(* *.*(..)) and @annotation(scheduled) and not @annotation(io.micrometer.core.annotation.Timed)")
	public Object timing(ProceedingJoinPoint pjp, Scheduled scheduled) throws Throwable {
		if (scheduled.cron().isEmpty())
			return pjp.proceed();
		Timed timed = AnnotationUtils.synthesizeAnnotation(Collections.singletonMap("longTask", true), Timed.class,
				((MethodSignature) pjp.getSignature()).getMethod());
		return timing(pjp, timed);
	}

	private static Object recordThrowable(LongTaskTimer timer, ThrowableCallable f) throws Throwable {
		LongTaskTimer.Sample timing = timer.start();
		try {
			return f.call();
		} finally {
			timing.stop();
		}
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