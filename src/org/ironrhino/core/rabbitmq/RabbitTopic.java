package org.ironrhino.core.rabbitmq;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.ironrhino.core.message.Topic;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class RabbitTopic<T extends Serializable> implements Topic<T> {

	@Autowired
	protected AmqpTemplate amqpTemplate;

	@Autowired
	protected RabbitAdmin rabbitAdmin;

	@Value("${rabbitmq.exchangeName:ironrhino}")
	protected String exchangeName;

	protected String routingKey = "";

	protected String queueName;

	@Autowired(required = false)
	private ExecutorService executorService;

	public String getQueueName() {
		return queueName;
	}

	public RabbitTopic() {
		Class<?> clazz = ReflectionUtils.getGenericClass(getClass(), RabbitTopic.class);
		if (clazz != null)
			routingKey = clazz.getName();
	}

	@PostConstruct
	public void init() {
		rabbitAdmin.declareExchange(new TopicExchange(exchangeName, true, false));
		Queue queue = rabbitAdmin.declareQueue();
		if (queue != null) {
			queueName = queue.getName();
			rabbitAdmin.declareBinding(
					new Binding(queueName, DestinationType.QUEUE, exchangeName, getRoutingKey(Scope.GLOBAL), null));
			rabbitAdmin.declareBinding(new Binding(queueName, DestinationType.QUEUE, exchangeName,
					getRoutingKey(Scope.APPLICATION), null));
		}
	}

	@PreDestroy
	public void destroy() {
		rabbitAdmin.deleteQueue(queueName);
	}

	protected String getRoutingKey(Scope scope) {
		if (scope == null || scope == Scope.LOCAL)
			return null;
		StringBuilder sb = new StringBuilder(routingKey).append(".");
		if (scope == Scope.APPLICATION)
			sb.append(AppInfo.getAppName());
		return sb.toString();
	}

	@Override
	public void publish(final T message, Scope scope) {
		if (scope == null)
			scope = Scope.GLOBAL;
		if (scope == Scope.LOCAL) {
			Runnable task = () -> subscribe(message);
			if (executorService != null)
				executorService.execute(task);
			else
				task.run();
		} else {
			amqpTemplate.convertAndSend(exchangeName, getRoutingKey(scope), message);
		}
	}
}
