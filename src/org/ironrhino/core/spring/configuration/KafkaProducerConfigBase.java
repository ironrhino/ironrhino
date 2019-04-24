package org.ironrhino.core.spring.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.ironrhino.core.metrics.KafkaProducerMetrics;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KafkaProducerConfigBase {

	@Value("${kafka.bootstrap.servers:localhost:9092}")
	private String bootstrapServers;

	@Value("${kafka.acks:all}")
	private String acks;

	@Value("${kafka.retries:3}")
	private int retries;

	@Value("${kafka.batch.size:16384}")
	private int batchSize;

	@Value("${kafka.max.block.ms:10000}")
	private int maxBlockMs;

	@Value("${kafka.linger.ms:1}")
	private int lingerMs;

	@Value("${kafka.buffer.memory:33554432}")
	private int bufferMemory;

	@Value("${kafka.fatal.if.broker.not.available:true}")
	private boolean fatalIfBrokerNotAvailable;

	@Bean
	@ClassPresentConditional("io.micrometer.core.instrument.Metrics")
	public KafkaProducerMetrics kafkaProducerMetrics() {
		return new KafkaProducerMetrics();
	}

	@Bean
	public <T> ProducerFactory<String, T> kafkaProducerFactory() {
		Map<String, Object> producerConfigs = new HashMap<>();
		producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
		producerConfigs.put(ProducerConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true));
		producerConfigs.put(ProducerConfig.ACKS_CONFIG, getAcks());
		producerConfigs.put(ProducerConfig.RETRIES_CONFIG, getRetries());
		producerConfigs.put(ProducerConfig.BATCH_SIZE_CONFIG, getBatchSize());
		producerConfigs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, getMaxBlockMs());
		producerConfigs.put(ProducerConfig.LINGER_MS_CONFIG, getLingerMs());
		producerConfigs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, getBufferMemory());
		DefaultKafkaProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);
		producerFactory.setKeySerializer(new StringSerializer());
		JsonSerializer<T> serializer = new JsonSerializer<>();
		serializer.setAddTypeInfo(false);
		producerFactory.setValueSerializer(serializer);
		return producerFactory;
	}

	@Bean
	public <T> KafkaTemplate<String, T> kafkaTemplate() {
		return new KafkaTemplate<String, T>(kafkaProducerFactory());
	}

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> adminConfigs = new HashMap<>();
		adminConfigs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
		adminConfigs.put(AdminClientConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true).replaceAll(":", "_"));
		KafkaAdmin ka = new KafkaAdmin(adminConfigs);
		ka.setFatalIfBrokerNotAvailable(isFatalIfBrokerNotAvailable());
		return ka;
	}

}
