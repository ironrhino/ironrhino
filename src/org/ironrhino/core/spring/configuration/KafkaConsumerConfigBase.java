package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.ironrhino.core.metrics.KafkaConsumerMetrics;
import org.ironrhino.core.spring.DefaultPropertiesProvider;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.opentracing.contrib.kafka.spring.TracingConsumerFactory;
import io.opentracing.util.GlobalTracer;
import lombok.Getter;

@EnableKafka
public class KafkaConsumerConfigBase implements DefaultPropertiesProvider {

	@Autowired
	private Environment environment;

	@Getter
	@Value("${kafka.consumer.concurrency:1}")
	private int consumerConcurrency = 1;

	@Getter
	private final Map<String, String> defaultProperties;

	public KafkaConsumerConfigBase() {
		Map<String, String> map = new HashMap<>();
		map.put(getConfigKeyPrefix() + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		map.put(getConfigKeyPrefix() + ConsumerConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true));
		map.put(getConfigKeyPrefix() + ConsumerConfig.GROUP_ID_CONFIG, AppInfo.getAppName());
		map.put(getConfigKeyPrefix() + ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
		defaultProperties = Collections.unmodifiableMap(map);
	}

	protected String getConfigKeyPrefix() {
		return "kafka.";
	}

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
		for (Field f : ConsumerConfig.class.getFields()) {
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isFinal(mod) && f.getType() == String.class
					&& !f.getName().endsWith("_DOC")) {
				try {
					String key = (String) f.get(null);
					if (key.equals(ConsumerConfig.DEFAULT_ISOLATION_LEVEL))
						continue;
					String prefixedKey = getConfigKeyPrefix() + key;
					String value = environment.getProperty(prefixedKey);
					if (value == null)
						value = defaultProperties.get(prefixedKey);
					if (value != null)
						consumerConfigs.put(key, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		consumerConfigs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, autoCommit);
		if (!autoCommit)
			consumerConfigs.remove(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG);
		DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerConfigs);
		consumerFactory.setKeyDeserializer(new StringDeserializer());
		consumerFactory.setValueDeserializer(new TopicNameBasedJsonDeserializer<>(JsonUtils.createNewObjectMapper()));
		if (Tracing.isEnabled()) {
			return new TracingConsumerFactory<String, T>(consumerFactory, GlobalTracer.get());
		}
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
