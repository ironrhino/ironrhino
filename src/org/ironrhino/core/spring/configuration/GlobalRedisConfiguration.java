package org.ironrhino.core.spring.configuration;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Order(0)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ApplicationContextPropertiesConditional(key = "global.redis.enabled", value = "true")
@ClassPresentConditional("org.springframework.data.redis.connection.RedisConnectionFactory")
@ConfigurationProperties(prefix = "global.redis")
public class GlobalRedisConfiguration extends RedisConfiguration {

	@Override
	@Bean(name = "globalRedisConnectionFactory")
	public RedisConnectionFactory redisConnectionFactory() {
		return super.redisConnectionFactory();
	}

	@Override
	@Bean(name = "globalRedisTemplate")
	public RedisTemplate<String, ?> redisTemplate() {
		return super.redisTemplate();
	}

	@Override
	@Bean(name = "globalStringRedisTemplate")
	public StringRedisTemplate stringRedisTemplate() {
		return super.stringRedisTemplate();
	}

	@Override
	@Bean(name = "globalRedisMessageListenerContainer")
	public RedisMessageListenerContainer redisMessageListenerContainer(
			@Autowired(required = false) ExecutorService executorService) {
		return super.redisMessageListenerContainer(executorService);
	}

}
