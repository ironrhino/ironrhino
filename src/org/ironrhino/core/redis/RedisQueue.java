package org.ironrhino.core.redis;

import java.io.Serializable;
import java.util.concurrent.BlockingDeque;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;

public abstract class RedisQueue<T extends Serializable> implements
		org.ironrhino.core.message.Queue<T> {

	protected String queueName;

	@Autowired
	private RedisTemplate<String, T> redisTemplate;

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public void setRedisTemplate(RedisTemplate<String, T> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	protected BlockingDeque<T> queue;

	public RedisQueue() {
		Class<?> clazz = ReflectionUtils.getGenericClass(getClass());
		queueName = clazz.getName();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		queue = new DefaultRedisList<T>(queueName, redisTemplate);

	}

	@Override
	public void produce(T message) {
		queue.add(message);
	}

}
