package org.ironrhino.sample.kafka.consumer;

import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.spring.configuration.KafkaComsumerConfigBase;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(Profiles.SANDBOX)
@Configuration
public class KafkaComsumerConfiguration extends KafkaComsumerConfigBase {

}
