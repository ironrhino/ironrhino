package org.ironrhino.core.redis;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@ApplicationContextPropertiesConditional(key = "redisKeyspaceNotifier.enabled", value = "true")
@Component
@Slf4j
public class RedisKeyspaceNotifier {

	@Value("${redisKeyspaceNotifier.eventBased:false}")
	private boolean eventBased;

	@Autowired
	private RedisMessageListenerContainer messageListenerContainer;

	@Autowired(required = false)
	private List<RedisKeyspaceEventListener> listeners = Collections.emptyList();

	@PostConstruct
	public void afterPropertiesSet() {
		if (listeners.isEmpty())
			return;
		LettuceConnectionFactory connectionFactory = (LettuceConnectionFactory) messageListenerContainer
				.getConnectionFactory();
		if (connectionFactory == null)
			return;
		int database = connectionFactory.getDatabase();
		String pattern = "__key" + (eventBased ? "event" : "space") + "@" + database + "__:*";
		// notify-keyspace-events KEg$x
		messageListenerContainer.addMessageListener((message, p) -> {
			String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
			String key = channel.substring(channel.indexOf(':') + 1);
			String event = new String(message.getBody(), StandardCharsets.UTF_8);
			if (eventBased) {
				String temp = event;
				event = key;
				key = temp;
			}
			String _event = event;
			String _key = key;
			listeners.forEach(listeners -> {
				try {
					listeners.onEvent(_event, _key);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			});
		}, Collections.singleton(new PatternTopic(pattern)));
	}

}