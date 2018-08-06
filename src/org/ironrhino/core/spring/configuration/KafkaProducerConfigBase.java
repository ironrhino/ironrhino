package org.ironrhino.core.spring.configuration;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
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

	@Value("${kafka.batchSize:16384}")
	private int batchSize;

	@Value("${kafka.maxBlockMs:10000}")
	private int maxBlockMs;

	@Value("${kafka.lingerMs:1}")
	private int lingerMs;

	@Value("${kafka.bufferMemory:33554432}")
	private int bufferMemory;

	@Value("${kafka.fatalIfBrokerNotAvailable:true}")
	private boolean fatalIfBrokerNotAvailable;

	@Value("${kafka.numPartitions:1}")
	private int numPartitions;

	@Value("${kafka.replicationFactor:1}")
	private short replicationFactor;

	@Bean
	public <T> ProducerFactory<String, T> kafkaProducerFactory() {
		Map<String, Object> producerConfigs = new HashMap<>();
		producerConfigs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		producerConfigs.put(ProducerConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true));
		producerConfigs.put(ProducerConfig.ACKS_CONFIG, acks);
		producerConfigs.put(ProducerConfig.RETRIES_CONFIG, retries);
		producerConfigs.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
		producerConfigs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
		producerConfigs.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
		producerConfigs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
		DefaultKafkaProducerFactory<String, T> producerFactory = new DefaultKafkaProducerFactory<>(producerConfigs);
		producerFactory.setKeySerializer(new StringSerializer());
		JsonSerializer<T> serializer = new JsonSerializer<>();
		serializer.setAddTypeInfo(false);
		producerFactory.setValueSerializer(serializer);
		return producerFactory;
	}

	@Bean
	public <T> KafkaTemplate<String, T> kafkaTemplate(ProducerFactory<String, T> producerFactory) {
		return new KafkaTemplate<String, T>(producerFactory);
	}

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> adminConfigs = new HashMap<>();
		adminConfigs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		adminConfigs.put(AdminClientConfig.CLIENT_ID_CONFIG, AppInfo.getInstanceId(true).replaceAll(":", "_"));
		KafkaAdmin ka = new KafkaAdmin(adminConfigs);
		ka.setFatalIfBrokerNotAvailable(fatalIfBrokerNotAvailable);
		return ka;
	}

}
