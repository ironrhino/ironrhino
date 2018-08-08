package org.ironrhino.core.metrics;

import static java.util.Collections.emptyList;

import javax.management.ObjectName;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

public class KafkaProducerMetrics extends JmxBasedMeterBinder {

	public KafkaProducerMetrics() {
		super();
	}

	public KafkaProducerMetrics(Iterable<Tag> tags) {
		super(tags);
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		registerProducerMetrics(registry);
		registerProducerTopicMetrics(registry);
	}

	private void registerProducerMetrics(MeterRegistry registry) {
		registerMetricsEventually("kafka.producer", "producer-metrics", (name, allTags) -> {
			String[] gauges = new String[] { "connection-count", "io-ratio", "request-rate", "response-rate" };
			for (String gaugeName : gauges)
				Gauge.builder(toMetricName(gaugeName), mBeanServer,
						s -> safeDouble(() -> s.getAttribute(name, gaugeName))).tags(allTags).register(registry);

			String[] counters = new String[] { "iotime-total", "request-total", "response-total" };
			for (String counterName : counters)
				FunctionCounter
						.builder(toMetricName(counterName), mBeanServer,
								s -> safeDouble(() -> s.getAttribute(name, counterName)))
						.tags(allTags).register(registry);
		});
	}

	private void registerProducerTopicMetrics(MeterRegistry registry) {
		registerMetricsEventually("kafka.producer", "producer-topic-metrics", (name, allTags) -> {
			String[] gauges = new String[] { "byte-rate", "record-error-rate", "record-retry-rate",
					"record-send-rate" };
			for (String gaugeName : gauges)
				Gauge.builder(toMetricName(gaugeName), mBeanServer,
						s -> safeDouble(() -> s.getAttribute(name, gaugeName))).tags(allTags).register(registry);

			String[] counters = new String[] { "byte-total", "record-error-total", "record-retry-total",
					"record-send-total" };
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
		return "kafka.producer." + attributeName.replaceAll("-", ".");
	}

	@Override
	protected Iterable<Tag> nameTag(ObjectName objectName) {
		String clientId = objectName.getKeyProperty("client-id");
		if (clientId != null) {
			clientId = clientId.replaceAll("\"", "");
			String topic = objectName.getKeyProperty("topic");
			if (topic != null)
				return Tags.of("producer", clientId, "topic", topic);
			else
				return Tags.of("producer", clientId);
		} else {
			return emptyList();
		}
	}
}