package org.ironrhino.sample.kafka.consumer;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.sample.kafka.domain.Alert;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Profile(Profiles.SANDBOX)
@Component
public class AlertConsumer {

//	@KafkaListener(topics = "#{alertTopic.name()}", containerFactory = "alertListenerContainerFactory")
//	public void listen(ConsumerRecord<String, Alert> record) {
//		System.out.println("received " + record.key() + " : " + JsonUtils.toJson(record.value()));
//	}

	@KafkaListener(topics = "#{alertTopic.name()}", containerFactory = "alertManualAckBatchListenerContainerFactory")
	public void listenWithManualAck(List<ConsumerRecord<String, Alert>> records, Acknowledgment ack) {
		for (ConsumerRecord<String, Alert> record : records) {
			System.out.println("received " + record.key() + " with manual ack : " + JsonUtils.toJson(record.value()));
		}
		ack.acknowledge();
		System.out.println("batch acked");
	}

}