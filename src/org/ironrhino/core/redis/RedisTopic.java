package org.ironrhino.core.redis;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.serializer.SerializationException;

import lombok.Setter;

@SuppressWarnings("rawtypes")
public abstract class RedisTopic<T extends Serializable> implements org.ironrhino.core.message.Topic<T> {

	@Setter
	protected String channelName;

	@Setter
	@Autowired
	@PriorityQualifier("mqRedisTemplate")
	private RedisTemplate redisTemplate;

	@Autowired(required = false)
	@Qualifier("globalRedisTemplate")
	private RedisTemplate globalRedisTemplate;

	@Autowired
	@PriorityQualifier("mqRedisMessageListenerContainer")
	private RedisMessageListenerContainer messageListenerContainer;

	@Autowired(required = false)
	@Qualifier("globalRedisMessageListenerContainer")
	private RedisMessageListenerContainer globalRedisMessageListenerContainer;

	@Autowired(required = false)
	private ExecutorService executorService;

	public RedisTopic() {
		Class<?> clazz = ReflectionUtils.getGenericClass(getClass(), RedisTopic.class);
		channelName = clazz.getName();
	}

	@PostConstruct
	@SuppressWarnings("unchecked")
	public void afterPropertiesSet() {
		Topic globalTopic = new ChannelTopic(getChannelName(Scope.GLOBAL));
		Topic applicationTopic = new ChannelTopic(getChannelName(Scope.APPLICATION));
		if (globalRedisTemplate != null) {
			globalRedisMessageListenerContainer.addMessageListener((message, pattern) -> {
				try {
					subscribe((T) globalRedisTemplate.getValueSerializer().deserialize(message.getBody()));
				} catch (SerializationException e) {
					// message from other app
					if (!(e.getCause() instanceof ClassNotFoundException))
						throw e;
				}
			}, Arrays.asList(globalTopic));
			messageListenerContainer.addMessageListener((message, pattern) -> {
				try {
					subscribe((T) redisTemplate.getValueSerializer().deserialize(message.getBody()));
				} catch (SerializationException e) {
					// message from other app
					if (!(e.getCause() instanceof ClassNotFoundException))
						throw e;
				}
			}, Arrays.asList(applicationTopic));
		} else {
			messageListenerContainer.addMessageListener((message, pattern) -> {
				try {
					subscribe((T) redisTemplate.getValueSerializer().deserialize(message.getBody()));
				} catch (SerializationException e) {
					// message from other app
					if (!(e.getCause() instanceof ClassNotFoundException))
						throw e;
				}
			}, Arrays.asList(globalTopic, applicationTopic));
		}
	}

	protected String getChannelName(Scope scope) {
		if (scope == null || scope == Scope.LOCAL)
			return null;
		StringBuilder sb = new StringBuilder(channelName).append(".");
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
			if (globalRedisTemplate != null && scope == Scope.GLOBAL)
				globalRedisTemplate.convertAndSend(getChannelName(scope), message);
			else
				redisTemplate.convertAndSend(getChannelName(scope), message);
		}
	}

}
