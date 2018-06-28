package org.ironrhino.core.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.spring.NameGenerator;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ThrowableCallable;
import org.springframework.core.Ordered;
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
@ClassPresentConditional("io.micrometer.core.instrument.Metrics")
public class TimedAspect extends BaseAspect {

	public TimedAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 1;
	}

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
		String[] tags = timed.extraTags();
		if (tags.length > 0) {
			Map<String, Object> context = buildContext(pjp);
			for (int i = 0; i < tags.length; i += 2)
				tags[i + 1] = ExpressionUtils.evalString(tags[i + 1], context);
		}
		if (timed.longTask()) {
			LongTaskTimer longTaskTimer = LongTaskTimer.builder(name).tags(tags).register(registry);
			return recordThrowable(longTaskTimer, () -> recordThrowable(longTaskTimer, pjp::proceed));
		} else {
			Timer.Builder timerBuilder = Timer.builder(name).tags(tags);
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

	private static Object recordThrowable(LongTaskTimer timer, ThrowableCallable<Object, Throwable> f)
			throws Throwable {
		LongTaskTimer.Sample timing = timer.start();
		try {
			return f.call();
		} finally {
			timing.stop();
		}
	}

	private static Object recordThrowable(Timer timer, ThrowableCallable<Object, Throwable> f) throws Throwable {
		long start = System.nanoTime();
		try {
			return f.call();
		} finally {
			timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
		}
	}

}