package org.ironrhino.core.spring.configuration;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import redis.clients.jedis.JedisPoolConfig;

@Configuration
@ApplicationContextPropertiesConditional(key = "global.redis.enabled", value = "true")
@ClassPresentConditional("org.springframework.data.redis.connection.RedisConnectionFactory")
public class GlobalRedisConfiguration {

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

	@Bean
	public JedisConnectionFactory globalRedisConnectionFactory() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(maxTotal);
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(minIdle);
		JedisConnectionFactory jedisConnectionFactory;
		if (sentinels != null) {
			RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration(master, sentinels);
			jedisConnectionFactory = usePool ? new JedisConnectionFactory(redisSentinelConfiguration, poolConfig)
					: new JedisConnectionFactory(redisSentinelConfiguration);
		} else if (clusterNodes != null) {
			RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(clusterNodes);
			jedisConnectionFactory = usePool ? new JedisConnectionFactory(redisClusterConfiguration, poolConfig)
					: new JedisConnectionFactory(redisClusterConfiguration);
		} else {
			jedisConnectionFactory = usePool ? new JedisConnectionFactory(poolConfig) : new JedisConnectionFactory();
			if (StringUtils.isNotBlank(host))
				hostName = host;
			jedisConnectionFactory.setHostName(hostName);
			jedisConnectionFactory.setPort(port);
		}
		jedisConnectionFactory.setUsePool(usePool);
		jedisConnectionFactory.setDatabase(database);
		jedisConnectionFactory.setPassword(password);
		return jedisConnectionFactory;
	}

	@Bean
	public RedisTemplate<String, ?> globalRedisTemplate() {
		RedisTemplate<String, ?> template = new RedisTemplate<>();
		template.setConnectionFactory(globalRedisConnectionFactory());
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		return template;
	}

	@Bean
	public StringRedisTemplate globalStringRedisTemplate() {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(globalRedisConnectionFactory());
		return template;
	}

	@Bean
	public RedisMessageListenerContainer globalRedisMessageListenerContainer(ExecutorService executorService) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(globalRedisConnectionFactory());
		container.setTaskExecutor(executorService);
		return container;
	}

}
