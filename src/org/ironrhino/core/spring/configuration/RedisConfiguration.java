package org.ironrhino.core.spring.configuration;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
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

	@Value("${redis.sentinels:#{null}}")
	private Set<String> sentinels;

	@Value("${redis.clusterNodes:#{null}}")
	private Set<String> clusterNodes;

	@Value("${redis.master:master}")
	private String master;

	@Value("${redis.password:#{null}}")
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

	@Value("${redis.connectTimeout:2000}")
	private int connectTimeout = 2000;

	@Value("${redis.readTimeout:5000}")
	private int readTimeout = 5000;

	@Value("${redis.useSsl:false}")
	private boolean useSsl;

	@Bean
	@Primary
	public JedisConnectionFactory redisConnectionFactory() {
		JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder()
				.connectTimeout(Duration.ofMillis(connectTimeout)).readTimeout(Duration.ofMillis(readTimeout));
		if (useSsl)
			builder.useSsl();
		if (usePool) {
			JedisPoolConfig poolConfig = new JedisPoolConfig();
			poolConfig.setMaxTotal(maxTotal);
			poolConfig.setMaxIdle(maxIdle);
			poolConfig.setMinIdle(minIdle);
			builder.usePooling().poolConfig(poolConfig);
		}
		JedisClientConfiguration clientConfiguration = builder.build();
		JedisConnectionFactory jedisConnectionFactory;
		if (sentinels != null) {
			RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration(master, sentinels);
			sentinelConfiguration.setDatabase(database);
			if (StringUtils.isNotBlank(password))
				sentinelConfiguration.setPassword(RedisPassword.of(password));
			jedisConnectionFactory = new JedisConnectionFactory(sentinelConfiguration, clientConfiguration);
		} else if (clusterNodes != null) {
			RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(clusterNodes);
			if (StringUtils.isNotBlank(password))
				clusterConfiguration.setPassword(RedisPassword.of(password));
			jedisConnectionFactory = new JedisConnectionFactory(clusterConfiguration, clientConfiguration);
		} else {
			if (StringUtils.isNotBlank(host))
				hostName = host;
			RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(hostName, port);
			standaloneConfiguration.setDatabase(database);
			if (StringUtils.isNotBlank(password))
				standaloneConfiguration.setPassword(RedisPassword.of(password));
			jedisConnectionFactory = new JedisConnectionFactory(standaloneConfiguration, clientConfiguration);
		}
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
	public StringRedisTemplate stringRedisTemplate() {
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
