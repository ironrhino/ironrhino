package org.ironrhino.sample.kafka.consumer;

import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.spring.configuration.KafkaComsumerConfigBase;
import org.ironrhino.sample.kafka.domain.Alert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Profile(Profiles.SANDBOX)
@Configuration
public class KafkaComsumerConfiguration extends KafkaComsumerConfigBase {

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Alert> alertListenerContainerFactory() {
		return super.createListenerContainerFactory(new JsonDeserializer<>(Alert.class));
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Alert> alertManualAckBatchListenerContainerFactory() {
		return super.createManualAckBatchListenerContainerFactory(new JsonDeserializer<>(Alert.class));
	}

}
