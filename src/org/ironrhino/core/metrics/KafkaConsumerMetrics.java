package org.ironrhino.core.metrics;

import static java.util.Collections.emptyList;

import javax.management.ObjectName;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class KafkaConsumerMetrics extends JmxBasedMeterBinder {

	public KafkaConsumerMetrics() {
		super();
	}

	public KafkaConsumerMetrics(Iterable<Tag> tags) {
		super(tags);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		registerConsumerFetchMetrics(registry);
		registerConsumerCoordinatorMetrics(registry);
	}

	private void registerConsumerFetchMetrics(MeterRegistry registry) {
		registerMetricsEventually("kafka.consumer", "consumer-fetch-manager-metrics", (name, allTags) -> {
			String[] gauges = new String[] { "bytes-consumed-rate", "fetch-latency-avg", "fetch-latency-max",
					"fetch-rate", "fetch-size-avg", "fetch-size-max", "records-consumed-rate", "records-lag-max",
					"records-per-request-avg" };
			for (String gaugeName : gauges)
				Gauge.builder(toMetricName(gaugeName), mBeanServer,
						s -> safeDouble(() -> s.getAttribute(name, gaugeName))).tags(allTags).register(registry);

			String[] counters = new String[] { "bytes-consumed-total", "fetch-total", "records-consumed-total" };
			for (String counterName : counters)
				FunctionCounter
						.builder(toMetricName(counterName), mBeanServer,
								s -> safeDouble(() -> s.getAttribute(name, counterName)))
						.tags(allTags).register(registry);
		});
	}

	private void registerConsumerCoordinatorMetrics(MeterRegistry registry) {
		registerMetricsEventually("kafka.consumer", "consumer-coordinator-metrics", (name, allTags) -> {
			String[] gauges = new String[] { "assigned-partitions", "commit-latency-avg", "commit-latency-max",
					"commit-rate" };
			for (String gaugeName : gauges)
				Gauge.builder(toMetricName(gaugeName), mBeanServer,
						s -> safeDouble(() -> s.getAttribute(name, gaugeName))).tags(allTags).register(registry);

			String[] counters = new String[] { "commit-total" };
			for (String counterName : counters)
				FunctionCounter
						.builder(toMetricName(counterName), mBeanServer,
								s -> safeDouble(() -> s.getAttribute(name, counterName)))
						.tags(allTags).register(registry);
		});
	}

	private String toMetricName(String attributeName) {
		if (attributeName.endsWith("-total"))
			attributeName = attributeName.substring(0, attributeName.lastIndexOf('-'));
		return "kafka.consumer." + attributeName.replaceAll("-", ".");
	}

	@Override
	protected Iterable<Tag> nameTag(ObjectName objectName) {
		String clientId = objectName.getKeyProperty("client-id");
		if (clientId != null) {
			clientId = clientId.replaceAll("\"", "");
			String topic = objectName.getKeyProperty("topic");
			if (topic != null)
				return Tags.of("consumer", clientId, "topic", topic);
			else
				return Tags.of("consumer", clientId);
		} else {
			return emptyList();
		}
	}
}