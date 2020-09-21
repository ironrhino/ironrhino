package org.ironrhino.core.tracing;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.ironrhino.core.aop.BaseAspect;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ExpressionUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.Transactional;

import io.jaegertracing.internal.JaegerSpanContext;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

@Aspect
public class TracingAspect extends BaseAspect {

	private static final String TAG_NAME_CALLSITE = "callsite";

	private static final String TAG_NAME_PREFIX_PARAM = "param.";

	private static final String TAG_NAME_TX_READONLY = "tx.readonly";

	private static final String TAG_NAME_ENTITY = "entity";

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
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		String operationName = traced.operationName();
		if (StringUtils.isBlank(operationName))
			operationName = ReflectionUtils.stringify(method);
		Span span = tracer.buildSpan(operationName).start();
		Object result = null;
		try (Scope scope = tracer.activateSpan(span)) {
			if (isDebug()) {
				span.setTag(TAG_NAME_CALLSITE, getCallSite(method, pjp.getTarget()));
				String[] params = ReflectionUtils.getParameterNames(method);
				for (int i = 0; i < params.length; i++)
					span.setTag(TAG_NAME_PREFIX_PARAM + params[i], String.valueOf(pjp.getArgs()[i]));
			}
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
		if (!Tracing.isEnabled())
			return pjp.proceed();
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		List<Serializable> tags = new ArrayList<>();
		tags.add(Tags.COMPONENT.getKey());
		tags.add("tx");
		tags.add(TAG_NAME_TX_READONLY);
		tags.add(transactional.readOnly());
		if (isDebug()) {
			Object target = pjp.getTarget();
			tags.add(TAG_NAME_CALLSITE);
			tags.add(getCallSite(method, target));
			if (target instanceof BaseManager<?>) {
				tags.add(TAG_NAME_ENTITY);
				tags.add(((BaseManager<?>) target).getEntityClass().getName());
			}
			String[] params = ReflectionUtils.getParameterNames(method);
			for (int i = 0; i < params.length; i++) {
				tags.add(TAG_NAME_PREFIX_PARAM + params[i]);
				tags.add(String.valueOf(pjp.getArgs()[i]));
			}
		}
		return Tracing.executeCheckedCallable(ReflectionUtils.stringify(method), pjp::proceed,
				tags.toArray(new Serializable[tags.size()]));
	}

	private static boolean isDebug() {
		if (AppInfo.getStage() == Stage.DEVELOPMENT)
			return true;
		Span activeSpan = GlobalTracer.get().activeSpan();
		SpanContext ctx = activeSpan != null ? activeSpan.context() : null;
		return ctx instanceof JaegerSpanContext && ((JaegerSpanContext) ctx).isDebug();
	}

	private static String getCallSite(Method method, Object target) {
		// is Throwable faster than Thread.currentThread() ?
		StackTraceElement[] elements = new Throwable().getStackTrace();
		String targetClassName = target.getClass().getName();
		boolean found = false;
		for (StackTraceElement element : elements) {
			String className = element.getClassName();
			if (element.getMethodName().equals(method.getName()) && className.equals(targetClassName)
					|| className.startsWith(targetClassName + "$$")) { // $$EnhancerBySpringCGLIB$$
				found = true;
				continue;
			}
			if (found) {
				return element.toString();
			}
		}
		return null;
	}

}
