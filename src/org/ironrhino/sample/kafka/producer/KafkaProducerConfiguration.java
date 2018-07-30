package org.ironrhino.sample.kafka.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.spring.configuration.KafkaProducerConfigBase;
import org.ironrhino.sample.kafka.domain.Alert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(Profiles.SANDBOX)
@Configuration
public class KafkaProducerConfiguration extends KafkaProducerConfigBase {

	@Bean
	public NewTopic alertTopic() {
		return new NewTopic(Alert.class.getName(), getNumPartitions(), getReplicationFactor());
	}

}
