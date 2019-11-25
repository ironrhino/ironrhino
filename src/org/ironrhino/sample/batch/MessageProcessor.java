package org.ironrhino.sample.batch;

import org.ironrhino.core.util.Snowflake;
import org.ironrhino.sample.crud.Message;
import org.springframework.batch.item.ItemProcessor;

public class MessageProcessor implements ItemProcessor<Message, Message> {

	private final Snowflake snowflake = new Snowflake(0);

	@Override
	public Message process(Message message) throws Exception {
		message.setId(snowflake.nextId());
		return message;
	}

}
