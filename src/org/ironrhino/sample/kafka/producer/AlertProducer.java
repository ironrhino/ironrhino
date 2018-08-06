package org.ironrhino.sample.kafka.producer;

import java.util.UUID;

import org.apache.kafka.clients.admin.NewTopic;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.sample.kafka.domain.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Profile(Profiles.SANDBOX)
@Component
public class AlertProducer {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private NewTopic alertTopic;

	public void send(String content) {
		Alert alert = new Alert();
		alert.setId(UUID.randomUUID().toString());
		alert.setContent(content);
		kafkaTemplate.send(alertTopic.name(), alert.getId(), alert);
	}

	public void sendTo(String to, String content) {
		Alert alert = new Alert();
		alert.setId(UUID.randomUUID().toString());
		alert.setTo(to);
		alert.setContent(content);
		// assign partition base on "to" to guarantee order of message
		kafkaTemplate.send(alertTopic.name(), partition(alert.getTo()), alert.getId(), alert);
	}

	private int partition(String key) {
		return key.hashCode() % alertTopic.numPartitions();
	}

}