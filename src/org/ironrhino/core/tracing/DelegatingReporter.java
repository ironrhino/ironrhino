package org.ironrhino.core.tracing;

import java.util.function.Predicate;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.spi.Reporter;
import io.opentracing.tag.Tags;

public class DelegatingReporter implements Reporter {

	private final Reporter delegate;

	private final Predicate<JaegerSpan> predicate;

	public DelegatingReporter(Reporter delegate, Predicate<JaegerSpan> predicate) {
		this.delegate = delegate;
		this.predicate = predicate;
	}

	@Override
	public void report(JaegerSpan span) {
		if (span.context().isDebug() || Boolean.TRUE.equals(span.getTags().get(Tags.ERROR.getKey()))
				|| span.getTags().get(Tags.SAMPLING_PRIORITY.getKey()) != null || span.getTags().get("async") != null
				|| predicate.test(span))
			delegate.report(span);
	}

	@Override
	public void close() {
		delegate.close();
	}

}
