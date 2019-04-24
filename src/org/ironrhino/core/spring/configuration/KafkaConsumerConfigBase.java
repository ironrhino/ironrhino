package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ironrhino.core.metrics.KafkaConsumerMetrics;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@EnableKafka
@Getter
@Setter
public class KafkaConsumerConfigBase {

	@Value("${kafka.bootstrap.servers:localhost:9092}")
	private String bootstrapServers;

	@Value("${kafka.session.timeout.ms:30000}")
	private int sessionTimeoutMs;

	@Value("${kafka.auto.commit.interval.ms:1000}")
	private int autoCommitIntervalMs = 1000;

	@Value("${kafka.max.poll.interval.ms:300}")
	private int maxPollIntervalMs = 300;

	@Value("${kafka.max.poll.records:500}")
	private int maxPollRecords = 500;

	@Value("${kafka.consumer.concurrency:1}")
	private int consumerConcurrency = 1;

	@Value("${kafka.auto.offset.reset:latest}")
	private String autoOffsetReset = "";

	@Bean
	@ClassPresentConditional("io.micrometer.core.instrument.Metrics")
	public KafkaConsumerMetrics kafkaConsumerMetrics() {
		return new KafkaConsumerMetrics();
	}

	@Bean
	public <T> ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory() {
		ConsumerFactory<String, T> consumerFactory = createConsumerFactory(true);
		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setConcurrency(getConsumerConcurrency());
		return factory;
	}

	@Bean
	public <T> ConcurrentKafkaListenerContainerFactory<String, T> manualAckBatchKafkaListenerContainerFactory() {
		ConsumerFactory<String, T> consumerFactory = createConsumerFactory(false);
		ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setConcurrency(getConsumerConcurrency());
		factory.setBatchListener(true);
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
		return factory;
	}

	protected <T> ConsumerFactory<String, T> createConsumerFactory(boolean autoCommit) {
		Map<String, Object> consumerConfigs = new HashMap<>();
		consumerConfigs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
		consumerConfigs.put(ConsumerConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true));
		consumerConfigs.put(ConsumerConfig.GROUP_ID_CONFIG, AppInfo.getAppName());
		consumerConfigs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, getAutoOffsetReset());
		consumerConfigs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, getSessionTimeoutMs());
		consumerConfigs.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, getMaxPollIntervalMs());
		consumerConfigs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, getMaxPollRecords());
		consumerConfigs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);

		if (autoCommit)
			consumerConfigs.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, getAutoCommitIntervalMs());
		DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfigs);
		consumerFactory.setKeyDeserializer(new StringDeserializer());
		consumerFactory.setValueDeserializer(new TopicNameBasedJsonDeserializer<>(JsonUtils.createNewObjectMapper()));
		return consumerFactory;
	}

	@SuppressWarnings("unchecked")
	private static class TopicNameBasedJsonDeserializer<T> extends JsonDeserializer<T> {

		private Map<String, Class<?>> cache = new ConcurrentHashMap<>();

		public TopicNameBasedJsonDeserializer(ObjectMapper objectMapper) {
			super(objectMapper);
		}

		@Override
		public T deserialize(String topic, Headers headers, byte[] data) {
			if (data == null)
				return null;
			try {
				Class<?> clazz = cache.computeIfAbsent(topic, cls -> {
					try {
						return (Class<T>) Class.forName(cls);
					} catch (ClassNotFoundException e) {
						return Object.class;
					}
				});
				return clazz != Object.class ? (T) this.objectMapper.readValue(data, clazz)
						: super.deserialize(topic, headers, data);
			} catch (IOException e) {
				throw new SerializationException(
						"Can't deserialize data [" + Arrays.toString(data) + "] from topic [" + topic + "]", e);
			}
		}

		@Override
		public T deserialize(String topic, byte[] data) {
			if (data == null)
				return null;
			try {
				Class<?> clazz = cache.computeIfAbsent(topic, cls -> {
					try {
						return (Class<T>) Class.forName(cls);
					} catch (ClassNotFoundException e) {
						return Object.class;
					}
				});
				return clazz != Object.class ? (T) this.objectMapper.readValue(data, clazz)
						: super.deserialize(topic, data);
			} catch (IOException e) {
				throw new SerializationException(
						"Can't deserialize data [" + Arrays.toString(data) + "] from topic [" + topic + "]", e);
			}

		}

	}
}
