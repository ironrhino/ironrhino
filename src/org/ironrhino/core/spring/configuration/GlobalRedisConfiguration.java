package org.ironrhino.core.spring.configuration;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import lombok.Getter;
import lombok.Setter;

@Order(0)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@ApplicationContextPropertiesConditional(key = "global.redis.enabled", value = "true")
@ClassPresentConditional("org.springframework.data.redis.connection.RedisConnectionFactory")
@Getter
@Setter
public class GlobalRedisConfiguration extends RedisConfiguration {

	// alias for hostName
	@Value("${global.redis.host:}")
	private String host;

	@Value("${global.redis.hostName:localhost}")
	private String hostName;

	@Value("${global.redis.port:6379}")
	private int port;

	@Value("${global.redis.sentinels:#{null}}")
	private Set<String> sentinels;

	@Value("${global.redis.clusterNodes:#{null}}")
	private Set<String> clusterNodes;

	@Value("${global.redis.master:master}")
	private String master;

	@Value("${global.redis.password:#{null}}")
	private String password;

	@Value("${global.redis.usePool:true}")
	private boolean usePool;

	@Value("${global.redis.database:0}")
	private int database;

	@Value("${global.redis.maxTotal:50}")
	private int maxTotal;

	@Value("${global.redis.maxIdle:10}")
	private int maxIdle;

	@Value("${global.redis.minIdle:1}")
	private int minIdle;

	@Value("${global.redis.connectTimeout:2000}")
	private int connectTimeout = 2000;

	@Value("${global.redis.readTimeout:5000}")
	private int readTimeout = 5000;

	@Value("${global.redis.useSsl:false}")
	private boolean useSsl;

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
