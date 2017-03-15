package org.ironrhino.core.redis;

import java.io.Serializable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;

public abstract class RedisQueue<T extends Serializable> implements org.ironrhino.core.message.Queue<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected String queueName;

	protected boolean consuming;

	private volatile boolean stopConsuming;

	@Autowired(required = false)
	private ExecutorService executorService;

	@Autowired
	@PriorityQualifier({ "mqRedisTemplate", "globalRedisTemplate" })
	private RedisTemplate<String, T> redisTemplate;

	public void setConsuming(boolean consuming) {
		this.consuming = consuming;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public void setRedisTemplate(RedisTemplate<String, T> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	protected BlockingDeque<T> queue;

	public RedisQueue() {
		Class<?> clazz = ReflectionUtils.getGenericClass(getClass(), RedisQueue.class);
		queueName = clazz.getName();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		queue = new DefaultRedisList<>(queueName, redisTemplate);
		if (consuming) {
			Runnable task = () -> {
				while (!stopConsuming) {
					try {
						T message = queue.take();
						consume(message);
					} catch (Throwable e) {
						logger.error(e.getMessage(), e);
					}
				}
			};
			if (executorService != null)
				executorService.execute(task);
			else
				new Thread(task).start();
		}
	}

	@PreDestroy
	public void stop() {
		stopConsuming = true;
	}

	@Override
	public void produce(T message) {
		queue.add(message);
	}

}
