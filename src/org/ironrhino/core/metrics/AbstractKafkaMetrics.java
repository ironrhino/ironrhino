package org.ironrhino.core.metrics;

import static java.util.Collections.emptyList;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;

public abstract class AbstractKafkaMetrics extends JmxBasedMeterBinder {

	@Override
	public void bindTo(MeterRegistry registry) {
		gaugeConfig().forEach((k, v) -> {
			registerMetricsEventually(getDomain(), k, (name, allTags) -> {
				for (String gaugeName : v)
					Gauge.builder(toMetricName(gaugeName), mBeanServer,
							s -> safeDouble(() -> s.getAttribute(name, gaugeName))).tags(allTags).register(registry);
			});
		});
		timeGaugeConfig().forEach((k, v) -> {
			registerMetricsEventually(getDomain(), k, (name, allTags) -> {
				v.forEach((gaugeName, timeUnit) -> {
					TimeGauge
							.builder(toMetricName(gaugeName), mBeanServer, timeUnit,
									s -> safeDouble(() -> s.getAttribute(name, gaugeName)))
							.tags(allTags).register(registry);
				});
			});
		});
		counterConfig().forEach((k, v) -> {
			registerMetricsEventually(getDomain(), k, (name, allTags) -> {
				for (String counterName : v)
					FunctionCounter
							.builder(toMetricName(counterName), mBeanServer,
									s -> safeDouble(() -> s.getAttribute(name, counterName)))
							.tags(allTags).register(registry);
			});
		});
	}

	@Override
	protected Iterable<Tag> nameTag(ObjectName objectName) {
		String clientId = objectName.getKeyProperty("client-id");
		if (clientId != null) {
			clientId = clientId.replaceAll("\"", "");
			String topic = objectName.getKeyProperty("topic");
			if (topic != null)
				return Tags.of("client-id", clientId, "topic", topic.replaceAll("_", "."));
			else
				return Tags.of("client-id", clientId);
		} else {
			return emptyList();
		}
	}

	protected abstract String getDomain();

	protected abstract Map<String, String[]> gaugeConfig();

	protected abstract Map<String, Map<String, TimeUnit>> timeGaugeConfig();

	protected abstract Map<String, String[]> counterConfig();

	protected String toMetricName(String attributeName) {
		if (attributeName.endsWith("-total"))
			attributeName = attributeName.substring(0, attributeName.lastIndexOf('-'));
		return getDomain() + "." + attributeName.replaceAll("-", ".");
	}
}