package org.ironrhino.core.redis;

import java.io.Serializable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;

import lombok.Setter;

public abstract class RedisQueue<T extends Serializable> implements org.ironrhino.core.message.Queue<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Setter
	protected String queueName;

	@Setter
	protected boolean consuming;

	private AtomicBoolean stopConsuming = new AtomicBoolean();

	private Thread worker;

	@Setter
	@Autowired
	@PriorityQualifier({ "mqRedisTemplate", "globalRedisTemplate" })
	private RedisTemplate<String, T> mqRedisTemplate;

	protected BlockingDeque<T> queue;

	public RedisQueue() {
		Class<?> clazz = GenericTypeResolver.resolveTypeArgument(getClass(), RedisQueue.class);
		if (clazz == null)
			throw new IllegalArgumentException(getClass().getName() + " should be generic");
		queueName = clazz.getName();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		queue = new DefaultRedisList<>(queueName, mqRedisTemplate);
		if (consuming) {
			Runnable task = () -> {
				while (!stopConsuming.get()) {
					try {
						consume(queue.take());
					} catch (Throwable e) {
						if (Thread.interrupted())
							break;
						logger.error(e.getMessage(), e);
					}
				}
			};
			worker = new Thread(task, "redis-queue-consumer-" + getClass().getSimpleName());
			worker.setDaemon(true);
			worker.start();
		}
	}

	@PreDestroy
	public void stop() {
		if (stopConsuming.compareAndSet(false, true)) {
			if (worker != null)
				worker.interrupt();
		}
	}

	@Override
	public void produce(T message) {
		queue.add(message);
	}

}
