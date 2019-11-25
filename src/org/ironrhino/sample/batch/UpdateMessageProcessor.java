package org.ironrhino.sample.batch;

import java.time.LocalDateTime;

import org.ironrhino.sample.crud.Message;
import org.springframework.batch.item.ItemProcessor;

public class UpdateMessageProcessor implements ItemProcessor<Message, Message> {

	@Override
	public Message process(Message message) throws Exception {
		message.setContent(message.getContent() + " [edited]");
		message.setModifyDate(LocalDateTime.now());
		return message;
	}

}
