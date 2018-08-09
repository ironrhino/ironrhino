package org.ironrhino.core.metrics;

import java.util.HashMap;
import java.util.Map;

import io.micrometer.core.instrument.Tag;

public class KafkaConsumerMetrics extends AbstractKafkaMetrics {

	public KafkaConsumerMetrics() {
		super();
	}

	public KafkaConsumerMetrics(Iterable<Tag> tags) {
		super(tags);
	}

	@Override
	protected String getDomain() {
		return "kafka.consumer";
	}

	@Override
	protected Map<String, String[]> gaugeConfig() {
		Map<String, String[]> config = new HashMap<>();
		config.put("consumer-fetch-manager-metrics",
				new String[] { "bytes-consumed-rate", "fetch-latency-avg", "fetch-latency-max", "fetch-rate",
						"fetch-size-avg", "fetch-size-max", "records-consumed-rate", "records-lag-max",
						"records-per-request-avg" });
		config.put("consumer-coordinator-metrics",
				new String[] { "assigned-partitions", "commit-latency-avg", "commit-latency-max", "commit-rate" });
		return config;
	}

	@Override
	protected Map<String, String[]> counterConfig() {
		Map<String, String[]> config = new HashMap<>();
		config.put("consumer-fetch-manager-metrics",
				new String[] { "bytes-consumed-total", "fetch-total", "records-consumed-total" });
		config.put("consumer-coordinator-metrics", new String[] { "commit-total" });
		return config;
	}
}