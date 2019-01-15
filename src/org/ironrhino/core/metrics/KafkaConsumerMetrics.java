package org.ironrhino.core.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KafkaConsumerMetrics extends AbstractKafkaMetrics {

	@Override
	protected String getDomain() {
		return "kafka.consumer";
	}

	@Override
	protected Map<String, String[]> gaugeConfig() {
		Map<String, String[]> config = new HashMap<>();
		config.put("consumer-fetch-manager-metrics",
				new String[] { "bytes-consumed-rate", "fetch-latency-avg", "fetch-latency-max",
						"fetch-throttle-time-avg", "fetch-throttle-time-max", "fetch-rate", "fetch-size-avg",
						"fetch-size-max", "records-consumed-rate", "records-lag", "records-lag-avg", "records-lag-max",
						"records-lead", "records-lead-avg", "records-lead-min", "records-per-request-avg" });
		config.put("consumer-coordinator-metrics",
				new String[] { "assigned-partitions", "commit-latency-avg", "commit-latency-max", "commit-rate" });
		return config;
	}

	@Override
	protected Map<String, Map<String, TimeUnit>> timeGaugeConfig() {
		Map<String, Map<String, TimeUnit>> config = new HashMap<>();
		Map<String, TimeUnit> gauges = new HashMap<>();
		gauges.put("fetch-latency-avg", TimeUnit.MILLISECONDS);
		gauges.put("fetch-latency-max", TimeUnit.MILLISECONDS);
		gauges.put("fetch-throttle-time-avg", TimeUnit.MILLISECONDS);
		gauges.put("fetch-throttle-time-max", TimeUnit.MILLISECONDS);
		config.put("consumer-fetch-manager-metrics", gauges);
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