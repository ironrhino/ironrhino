package org.ironrhino.core.redis;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;

import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.spring.configuration.PriorityQualifier;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.serializer.SerializationException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Slf4j
public abstract class RedisTopic<T extends Serializable> implements org.ironrhino.core.message.Topic<T> {

	@Setter
	protected String channelName;

	@Setter
	@Autowired
	@PriorityQualifier
	private RedisTemplate mqRedisTemplate;

	@Autowired(required = false)
	@Qualifier("globalRedisTemplate")
	private RedisTemplate globalRedisTemplate;

	@Autowired
	@PriorityQualifier
	private RedisMessageListenerContainer mqRedisMessageListenerContainer;

	@Autowired(required = false)
	@Qualifier("globalRedisMessageListenerContainer")
	private RedisMessageListenerContainer globalRedisMessageListenerContainer;

	@Autowired(required = false)
	private ExecutorService executorService;

	public RedisTopic() {
		Class<?> clazz = GenericTypeResolver.resolveTypeArgument(getClass(), RedisTopic.class);
		if (clazz == null)
			throw new IllegalArgumentException(getClass().getName() + " should be generic");
		channelName = clazz.getName();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		Topic globalTopic = new ChannelTopic(getChannelName(Scope.GLOBAL));
		Topic applicationTopic = new ChannelTopic(getChannelName(Scope.APPLICATION));
		if (globalRedisTemplate != null) {
			doSubscribe(globalRedisMessageListenerContainer, globalRedisTemplate, globalTopic);
			doSubscribe(mqRedisMessageListenerContainer, mqRedisTemplate, applicationTopic);
		} else {
			doSubscribe(mqRedisMessageListenerContainer, mqRedisTemplate, globalTopic, applicationTopic);
		}
	}

	@SuppressWarnings("unchecked")
	private void doSubscribe(RedisMessageListenerContainer container, RedisTemplate template, Topic... topics) {
		container.addMessageListener((message, pattern) -> {
			try {
				T msg = (T) template.getValueSerializer().deserialize(message.getBody());
				log.info("Receive published message: {}", msg);
				subscribe(msg);
			} catch (SerializationException e) {
				// message from other app
				if (ExceptionUtils.getRootCause(e) instanceof ClassNotFoundException) {
					log.warn(e.getMessage());
				} else {
					throw e;
				}
			}
		}, Arrays.asList(topics));
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
	public void publish(T message, Scope scope) {
		if (scope == null)
			scope = Scope.GLOBAL;
		log.info("Publishing {} message: {}", scope.name(), message);
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
				mqRedisTemplate.convertAndSend(getChannelName(scope), message);
		}
	}

}
