package org.ironrhino.core.tracing;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.ironrhino.core.util.CheckedCallable;
import org.ironrhino.core.util.CheckedRunnable;
import org.springframework.util.ClassUtils;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Tracing {

	private static volatile boolean enabled = ClassUtils.isPresent("io.opentelemetry.api.trace.Tracer",
			Tracing.class.getClassLoader());

	static void disable() {
		enabled = false;
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static <T> T execute(String operationName, Callable<T> callable, Serializable... tags) throws Exception {
		if (!enabled || shouldSkip(tags))
			return callable.call();
		Span span = buildSpan(operationName, tags);
		try (Scope scope = span.makeCurrent()) {
			return callable.call();
		} catch (Exception ex) {
			logError(ex);
			throw ex;
		} finally {
			span.end();
		}
	}

	public static void execute(String operationName, Runnable runnable, Serializable... tags) {
		if (!enabled || shouldSkip(tags)) {
			runnable.run();
			return;
		}
		Span span = buildSpan(operationName, tags);
		try (Scope scope = span.makeCurrent()) {
			runnable.run();
		} catch (Exception ex) {
			logError(ex);
			throw ex;
		} finally {
			span.end();
		}
	}

	public static <T, E extends Throwable> T executeCheckedCallable(String operationName,
			CheckedCallable<T, E> callable, Serializable... tags) throws E {
		if (!enabled || shouldSkip(tags))
			return callable.call();
		Span span = buildSpan(operationName, tags);
		try (Scope scope = span.makeCurrent()) {
			return callable.call();
		} catch (Throwable ex) {
			logError(ex);
			throw ex;
		} finally {
			span.end();
		}
	}

	public static <E extends Throwable> void executeCheckedRunnable(String operationName, CheckedRunnable<E> runnable,
			Serializable... tags) throws E {
		if (!enabled || shouldSkip(tags)) {
			runnable.run();
			return;
		}
		Span span = buildSpan(operationName, tags);
		try (Scope scope = span.makeCurrent()) {
			runnable.run();
		} catch (Exception ex) {
			logError(ex);
			throw ex;
		} finally {
			span.end();
		}
	}

	public static <T> Callable<T> wrapAsync(String operationName, Callable<T> callable, Serializable... tags) {
		if (!enabled || shouldSkip(tags))
			return callable;
		ensureReportingActiveSpan();
		Span span = buildSpan(operationName, tags);
		span.setAttribute("async", true);
		return () -> {
			try (Scope scope = span.makeCurrent()) {
				return callable.call();
			} catch (Exception ex) {
				logError(ex);
				throw ex;
			} finally {
				span.end();
			}
		};
	}

	public static <T> Runnable wrapAsync(String operationName, Runnable runnable, Serializable... tags) {
		if (!enabled || shouldSkip(tags))
			return runnable;
		ensureReportingActiveSpan();
		Span span = buildSpan(operationName, tags);
		span.setAttribute("async", true);
		return () -> {
			try (Scope scope = span.makeCurrent()) {
				runnable.run();
			} catch (Exception ex) {
				logError(ex);
				throw ex;
			} finally {
				span.end();
			}
		};
	}

	public static <T, E extends Throwable> CheckedCallable<T, E> wrapAsyncCheckedCallable(String operationName,
			CheckedCallable<T, E> callable, Serializable... tags) {
		if (!enabled || shouldSkip(tags))
			return callable;
		ensureReportingActiveSpan();
		Span span = buildSpan(operationName, tags);
		span.setAttribute("async", true);
		return () -> {
			try (Scope scope = span.makeCurrent()) {
				return callable.call();
			} catch (Exception ex) {
				logError(ex);
				throw ex;
			} finally {
				span.end();
			}
		};
	}

	public static <E extends Throwable> CheckedRunnable<E> wrapAsyncCheckedRunnable(String operationName,
			CheckedRunnable<E> runnable, Serializable... tags) {
		if (!enabled || shouldSkip(tags))
			return runnable;
		ensureReportingActiveSpan();
		Span span = buildSpan(operationName, tags);
		span.setAttribute("async", true);
		return () -> {
			try (Scope scope = span.makeCurrent()) {
				runnable.run();
			} catch (Exception ex) {
				logError(ex);
				throw ex;
			} finally {
				span.end();
			}
		};
	}

	public static void logError(Throwable ex) {
		if (isCurrentSpanActive()) {
			Span span = Span.current();
			span.recordException(ex);
		}
	}

	public static boolean isCurrentSpanActive() {
		if (!enabled)
			return false;
		return Span.current().getSpanContext() != SpanContext.getInvalid();
	}

	private static boolean shouldSkip(Serializable... tags) {
		boolean isComponent = false;
		Integer samplingPriority = null;
		for (int i = 0; i < tags.length; i += 2) {
			if ("component".equals(tags[i])) {
				isComponent = true;
			} else if ("sample.priority".equals(tags[i])) {
				Serializable value = tags[i + 1];
				if (value instanceof Integer)
					samplingPriority = (Integer) value;
			}
		}
		return isCurrentSpanActive() && isComponent || (samplingPriority != null && samplingPriority < 0);
	}

	private static Span buildSpan(String operationName, Serializable... tags) {
		Tracer tracer = GlobalOpenTelemetry.getTracer("ironrhino");
		SpanBuilder spanBuilder = tracer.spanBuilder(operationName);
		Span span = spanBuilder.startSpan();
		setTags(span, tags);
		return span;
	}

	private static void setTags(Span span, Serializable... tags) {
		if (tags.length > 0) {
			if (tags.length % 2 != 0)
				throw new IllegalArgumentException("Tags should be key value pair");
			for (int i = 0; i < tags.length / 2; i++) {
				String name = String.valueOf(tags[i * 2]);
				Serializable value = tags[i * 2 + 1];
				if (value instanceof Long)
					span.setAttribute(name, (Long) value);
				else if (value instanceof Double)
					span.setAttribute(name, (Double) value);
				else if (value instanceof Boolean)
					span.setAttribute(name, (Boolean) value);
				else if (value != null)
					span.setAttribute(name, String.valueOf(value));
			}
		}
	}

	public static void ensureReportingActiveSpan() {
		if (!enabled)
			return;
		// ensure current span be reported
		// see also org.ironrhino.core.tracing.DelegatingReporter
		Span current = Span.current();
		if (isCurrentSpanActive())
			current.setAttribute("sample.priority", 1);
	}

	public static void setTags(Serializable... tags) {
		if (Tracing.isEnabled()) {
			Span span = Span.current();
			if (isCurrentSpanActive())
				setTags(span, tags);
		}
	}

}
