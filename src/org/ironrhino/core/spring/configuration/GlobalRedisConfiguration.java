package org.ironrhino.core.spring.configuration;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	@Value("${global.redis.connectTimeout:2000}")
	private int connectTimeout = 2000;

	@Value("${global.redis.readTimeout:5000}")
	private int readTimeout = 5000;

	@Value("${global.redis.useSsl:false}")
	private boolean useSsl;

	@Bean
	public JedisConnectionFactory globalRedisConnectionFactory() {
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
