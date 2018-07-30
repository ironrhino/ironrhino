package org.ironrhino.core.spring.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;

@EnableKafka
public class KafkaComsumerConfigBase {

	@Value("${kafka.bootstrap.servers:localhost:9092}")
	private String bootstrapServers;

	@Value("${kafka.sessionTimeoutMs:30000}")
	private int sessionTimeoutMs;

	@Value("${kafka.autoCommitIntervalMs:1000}")
	private int autoCommitIntervalMs = 1000;

	@Value("${kafka.consumerConcurrency:1}")
	private int consumerConcurrency = 1;

	@Value("${kafka.autoOffsetReset:earliest}")
	private String autoOffsetReset = "";

	protected <T> ConcurrentKafkaListenerContainerFactory<String, T> createListenerContainerFactory(
			Deserializer<T> valueDeserializer) {
		ConsumerFactory<String, T> consumerFactory = createConsumerFactory(valueDeserializer, true);
		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setConcurrency(consumerConcurrency);
		return factory;
	}

	protected <T> ConcurrentKafkaListenerContainerFactory<String, T> createManualAckBatchListenerContainerFactory(
			Deserializer<T> valueDeserializer) {
		ConsumerFactory<String, T> consumerFactory = createConsumerFactory(valueDeserializer, false);
		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setConcurrency(consumerConcurrency);
		factory.setBatchListener(true);
		factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
		return factory;
	}

	protected <T> ConsumerFactory<String, T> createConsumerFactory(Deserializer<T> valueDeserializer,
			boolean autoCommit) {
		Map<String, Object> consumerConfigs = new HashMap<>();
		consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		consumerConfigs.put(ConsumerConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true).replaceAll(":", "_"));
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, AppInfo.getAppName());
		consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
		consumerConfigs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
		consumerConfigs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
		if (autoCommit)
			consumerConfigs.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitIntervalMs);
		DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfigs);
		consumerFactory.setKeyDeserializer(new StringDeserializer());
		consumerFactory.setValueDeserializer(valueDeserializer);
		return consumerFactory;
	}

}
