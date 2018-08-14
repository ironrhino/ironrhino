package org.ironrhino.sample.kafka.consumer;

import java.util.Date;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.sample.kafka.domain.Alert;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Profile(Profiles.SANDBOX)
@Component
@Slf4j
public class AlertConsumer {

//	@KafkaListener(topics = "#{alertTopic.name()}", containerFactory = "kafkaListenerContainerFactory")
//	public void listen(ConsumerRecord<String, Alert> record) {
//		process(record);
//	}

	@KafkaListener(topics = "#{alertTopic.name()}", containerFactory = "manualAckBatchKafkaListenerContainerFactory")
	public void listenWithManualAck(List<ConsumerRecord<String, Alert>> records, Acknowledgment ack) {
		for (ConsumerRecord<String, Alert> record : records) {
			process(record);
		}
		ack.acknowledge();
		log.info("batch acked");
	}

	private void process(ConsumerRecord<String, Alert> record) {
		log.info("received from {}-{} with key {} created at {} : {}", record.topic(), record.partition(), record.key(),
				DateUtils.formatTimestamp(new Date(record.timestamp())), JsonUtils.toJson(record.value()));
	}

}