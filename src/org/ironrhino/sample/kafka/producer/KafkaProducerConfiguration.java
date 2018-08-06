package org.ironrhino.sample.kafka.producer;

import org.apache.kafka.clients.admin.NewTopic;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.spring.configuration.KafkaProducerConfigBase;
import org.ironrhino.sample.kafka.domain.Alert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(Profiles.SANDBOX)
@Configuration
public class KafkaProducerConfiguration extends KafkaProducerConfigBase {

	@Bean
	public NewTopic alertTopic(@Value("${kafka.alertTopic.numPartitions:4}") int numPartitions,
			@Value("${kafka.alertTopic.replicationFactor:1}") short replicationFactor) {
		return new NewTopic(Alert.class.getName(), numPartitions, replicationFactor);
	}

}
