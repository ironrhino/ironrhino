package org.ironrhino.core.spring.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.ironrhino.core.metrics.KafkaProducerMetrics;
import org.ironrhino.core.spring.DefaultPropertiesProvider;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import io.opentracing.contrib.kafka.spring.TracingProducerFactory;
import io.opentracing.util.GlobalTracer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaProducerConfigBase implements DefaultPropertiesProvider {

	@Autowired
	private Environment environment;

	@Value("${kafka.fatal.if.broker.not.available:true}")
	private boolean fatalIfBrokerNotAvailable;

	@Getter
	private final Map<String, String> defaultProperties;

	public KafkaProducerConfigBase() {
		Map<String, String> map = new HashMap<>();
		map.put(getConfigKeyPrefix() + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		map.put(getConfigKeyPrefix() + ProducerConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true));
		defaultProperties = Collections.unmodifiableMap(map);
	}

	protected String getConfigKeyPrefix() {
		return "kafka.";
	}

	@Bean
	@ClassPresentConditional("io.micrometer.core.instrument.Metrics")
	public KafkaProducerMetrics kafkaProducerMetrics() {
		return new KafkaProducerMetrics();
	}

	@Bean
	public <T> ProducerFactory<String, T> kafkaProducerFactory() {
		Map<String, Object> producerConfigs = new HashMap<>();
		for (Field f : ProducerConfig.class.getFields()) {
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isFinal(mod) && f.getType() == String.class
					&& !f.getName().endsWith("_DOC")) {
				try {
					String key = (String) f.get(null);
					String prefixedKey = getConfigKeyPrefix() + key;
					String value = environment.getProperty(prefixedKey);
					if (value == null)
						value = defaultProperties.get(prefixedKey);
					if (value != null)
						producerConfigs.put(key, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		DefaultKafkaProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);
		producerFactory.setKeySerializer(new StringSerializer());
		JsonSerializer<T> serializer = new JsonSerializer<>();
		serializer.setAddTypeInfo(false);
		producerFactory.setValueSerializer(serializer);
		return Tracing.isEnabled() ? new TracingProducerFactory<String, T>(producerFactory, GlobalTracer.get())
				: producerFactory;
	}

	@Bean
	public <T> KafkaTemplate<String, T> kafkaTemplate() {
		return new KafkaTemplate<String, T>(kafkaProducerFactory());
	}

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> adminConfigs = new HashMap<>();
		for (Field f : AdminClientConfig.class.getFields()) {
			int mod = f.getModifiers();
			if (Modifier.isStatic(mod) && Modifier.isFinal(mod) && f.getType() == String.class
					&& !f.getName().endsWith("_DOC")) {
				try {
					String key = (String) f.get(null);
					String prefixedKey = getConfigKeyPrefix() + key;
					String value = environment.getProperty(prefixedKey);
					if (value == null)
						value = defaultProperties.get(prefixedKey);
					if (value != null)
						adminConfigs.put(key, value);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		KafkaAdmin ka = new KafkaAdmin(adminConfigs);
		ka.setFatalIfBrokerNotAvailable(isFatalIfBrokerNotAvailable());
		return ka;
	}

}
