package org.ironrhino.core.tracing;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Transactional;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

@Aspect
public class TracingAspect extends BaseAspect {

	public TracingAspect() {
		order = Ordered.HIGHEST_PRECEDENCE + 1;
	}

	@Around("execution(public * *(..)) and @annotation(traced)")
	public Object trace(ProceedingJoinPoint pjp, Traced traced) throws Throwable {
		if (!Tracing.isEnabled())
			return pjp.proceed();
		Tracer tracer = GlobalTracer.get();
		if (traced.withActiveSpanOnly() && tracer.activeSpan() == null)
			return pjp.proceed();
		String operationName = traced.operationName();
		if (StringUtils.isBlank(operationName))
			operationName = ReflectionUtils.stringify(((MethodSignature) pjp.getSignature()).getMethod());
		Span span = tracer.buildSpan(operationName).start();
		Object result = null;
		try (Scope scope = tracer.activateSpan(span)) {
			result = pjp.proceed();
			return result;
		} catch (Exception ex) {
			Tracing.logError(ex);
			throw ex;
		} finally {
			Map<String, Object> context = buildContext(pjp);
			putReturnValueIntoContext(context, result);
			for (Tag tag : traced.tags())
				span.setTag(tag.name(), ExpressionUtils.evalString(tag.value(), context));
			span.finish();
		}
	}

	@Around("execution(public * *(..)) and @annotation(transactional) and not @annotation(org.ironrhino.core.tracing.Traced)")
	public Object trace(ProceedingJoinPoint pjp, Transactional transactional) throws Throwable {
		if (GlobalTracer.get().activeSpan() == null)
			return pjp.proceed();
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		return Tracing.executeCheckedCallable(ReflectionUtils.stringify(method), pjp::proceed, "transaction.readonly",
				transactional.readOnly());
	}

}
