package org.ironrhino.core.metrics;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.FunctionTimer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;

public class TomcatMetrics extends JmxBasedMeterBinder {

	public static void monitor(MeterRegistry registry) {
		new TomcatMetrics().bindTo(registry);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		registerGlobalRequestMetrics(registry);
		registerThreadPoolMetrics(registry);
	}

	private void registerThreadPoolMetrics(MeterRegistry registry) {
		registerMetricsEventually("Catalina", "ThreadPool", (name, allTags) -> {
			Gauge.builder("tomcat.threads.busy", mBeanServer,
					s -> safeDouble(() -> s.getAttribute(name, "currentThreadsBusy"))).tags(allTags).register(registry);

			Gauge.builder("tomcat.threads.current", mBeanServer,
					s -> safeDouble(() -> s.getAttribute(name, "currentThreadCount"))).tags(allTags).register(registry);
		});
	}

	private void registerGlobalRequestMetrics(MeterRegistry registry) {
		registerMetricsEventually("Catalina", "GlobalRequestProcessor", (name, allTags) -> {
			FunctionCounter
					.builder("tomcat.global.sent", mBeanServer,
							s -> safeDouble(() -> s.getAttribute(name, "bytesSent")))
					.tags(allTags).baseUnit("bytes").register(registry);

			FunctionCounter
					.builder("tomcat.global.received", mBeanServer,
							s -> safeDouble(() -> s.getAttribute(name, "bytesReceived")))
					.tags(allTags).baseUnit("bytes").register(registry);

			FunctionCounter.builder("tomcat.global.error", mBeanServer,
					s -> safeDouble(() -> s.getAttribute(name, "errorCount"))).tags(allTags).register(registry);

			FunctionTimer
					.builder("tomcat.global.request", mBeanServer,
							s -> safeLong(() -> s.getAttribute(name, "requestCount")),
							s -> safeDouble(() -> s.getAttribute(name, "processingTime")), TimeUnit.MILLISECONDS)
					.tags(allTags).register(registry);

			TimeGauge.builder("tomcat.global.request.max", mBeanServer, TimeUnit.MILLISECONDS,
					s -> safeDouble(() -> s.getAttribute(name, "maxTime"))).tags(allTags).register(registry);
		});
	}

	@Override
	protected Iterable<Tag> nameTag(ObjectName objectName) {
		String name = objectName.getKeyProperty("name");
		if (name != null) {
			return Tags.of("name", name.replaceAll("\"", ""));
		} else {
			return Collections.emptyList();
		}
	}
}
