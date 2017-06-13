package org.ironrhino.core.spring.configuration;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
@Profile({ DUAL, CLUSTER, CLOUD, "redis" })
@ClassPresentConditional("org.springframework.data.redis.connection.RedisConnectionFactory")
public class RedisConfiguration {

	// alias for hostName
	@Value("${redis.host:}")
	private String host;

	@Value("${redis.hostName:localhost}")
	private String hostName;

	@Value("${redis.port:6379}")
	private int port;

	@Value("${redis.sentinels:}")
	private Set<String> sentinels;

	@Value("${redis.clusterNodes:}")
	private Set<String> clusterNodes;

	@Value("${redis.master:master}")
	private String master;

	@Value("${redis.password:}")
	private String password;

	@Value("${redis.usePool:true}")
	private boolean usePool;

	@Value("${redis.database:0}")
	private int database;

	@Value("${redis.maxTotal:50}")
	private int maxTotal;

	@Value("${redis.maxIdle:10}")
	private int maxIdle;

	@Value("${redis.minIdle:1}")
	private int minIdle;

	@Bean
	@Primary
	public JedisConnectionFactory redisConnectionFactory() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(maxTotal);
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(minIdle);
		JedisConnectionFactory jedisConnectionFactory;
		if (sentinels != null && !sentinels.isEmpty()) {
			RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration(master, sentinels);
			jedisConnectionFactory = usePool ? new JedisConnectionFactory(redisSentinelConfiguration, poolConfig)
					: new JedisConnectionFactory(redisSentinelConfiguration);
		} else if (clusterNodes != null && !clusterNodes.isEmpty()) {
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
	@Primary
	public RedisTemplate<String, ?> redisTemplate() {
		RedisTemplate<String, ?> template = new RedisTemplate<>();
		template.setConnectionFactory(redisConnectionFactory());
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		return template;
	}

	@Bean
	public RedisTemplate<String, ?> stringRedisTemplate() {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(redisConnectionFactory());
		return template;
	}

	@Bean
	@Primary
	public RedisMessageListenerContainer redisMessageListenerContainer(ExecutorService executorService) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory());
		container.setTaskExecutor(executorService);
		return container;
	}

}
