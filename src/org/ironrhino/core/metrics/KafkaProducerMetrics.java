package org.ironrhino.core.metrics;

import java.util.HashMap;
import java.util.Map;

public class KafkaProducerMetrics extends AbstractKafkaMetrics {

	@Override
	protected String getDomain() {
		return "kafka.producer";
	}

	@Override
	protected Map<String, String[]> gaugeConfig() {
		Map<String, String[]> config = new HashMap<>();
		config.put("producer-metrics",
				new String[] { "connection-count", "io-ratio", "request-rate", "response-rate" });
		config.put("producer-topic-metrics",
				new String[] { "byte-rate", "record-error-rate", "record-retry-rate", "record-send-rate" });
		return config;
	}

	@Override
	protected Map<String, String[]> counterConfig() {
		Map<String, String[]> config = new HashMap<>();
		config.put("producer-metrics", new String[] { "iotime-total", "request-total", "response-total" });
		config.put("producer-topic-metrics",
				new String[] { "byte-total", "record-error-total", "record-retry-total", "record-send-total" });
		return config;
	}

}