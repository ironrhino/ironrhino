package org.ironrhino.core.rabbitmq;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import org.ironrhino.core.message.Queue;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

public abstract class RabbitQueue<T extends Serializable> implements Queue<T> {

	@Autowired
	protected AmqpTemplate amqpTemplate;

	@Autowired
	protected RabbitAdmin rabbitAdmin;

	protected String queueName = "";

	protected boolean durable = true;

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public RabbitQueue() {
		Class<?> clazz = ReflectionUtils.getGenericClass(getClass());
		if (clazz != null)
			queueName = clazz.getName();
	}

	@PostConstruct
	public void init() {
		rabbitAdmin.declareQueue(new org.springframework.amqp.core.Queue(
				queueName, durable, false, false));
	}

	@Override
	public void produce(T message) {
		amqpTemplate.convertAndSend(getQueueName(), "", message);
	}

}
