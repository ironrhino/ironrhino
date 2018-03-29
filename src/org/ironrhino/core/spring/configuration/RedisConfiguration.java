package org.ironrhino.core.spring.configuration;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Profile({ DUAL, CLUSTER, CLOUD, "redis" })
@ClassPresentConditional("org.springframework.data.redis.connection.RedisConnectionFactory")
public class RedisConfiguration extends RedisConfigurationBase {

	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		return super.createRedisConnectionFactory();
	}

	@Bean
	@Primary
	public RedisTemplate<String, ?> redisTemplate() {
		return super.createRedisTemplate(redisConnectionFactory());
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate() {
		return super.createStringRedisTemplate(redisConnectionFactory());
	}

	@Bean
	@Primary
	public RedisMessageListenerContainer redisMessageListenerContainer(
			@Autowired(required = false) ExecutorService executorService) {
		return super.createRedisMessageListenerContainer(redisConnectionFactory(), executorService);
	}

}
