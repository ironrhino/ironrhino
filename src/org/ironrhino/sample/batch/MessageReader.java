package org.ironrhino.sample.batch;

import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.ironrhino.sample.crud.Message;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

public class MessageReader implements ItemReader<Message> {

	private final AtomicInteger counter = new AtomicInteger();

	@Value("#{jobParameters[createDate]}")
	private Date createDate;

	@Value("#{jobParameters[count]}")
	private int count;

	@Override
	public Message read() throws Exception {
		int current = counter.getAndIncrement();
		if (current >= count)
			return null;
		Message msg = new Message();
		msg.setTitle("message " + current);
		msg.setContent("content " + current);
		msg.setCreateDate(createDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
		return msg;
	}

}
