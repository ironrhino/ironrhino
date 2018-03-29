package org.ironrhino.core.spring.configuration;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.Getter;
import lombok.Setter;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Profile({ DUAL, CLUSTER, CLOUD, "redis" })
@ClassPresentConditional("org.springframework.data.redis.connection.RedisConnectionFactory")
@Getter
@Setter
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
	public RedisConnectionFactory redisConnectionFactory() {
		ClientOptions clientOptions = ClientOptions.builder()
				.socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(getConnectTimeout())).build())
				.build();
		LettuceClientConfigurationBuilder builder;
		if (isUsePool()) {
			GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
			poolConfig.setMaxTotal(getMaxTotal());
			poolConfig.setMaxIdle(getMaxIdle());
			poolConfig.setMinIdle(getMinIdle());
			builder = LettucePoolingClientConfiguration.builder().poolConfig(poolConfig);
		} else {
			builder = LettuceClientConfiguration.builder();
		}
		if (isUseSsl())
			builder.useSsl();
		LettuceClientConfiguration clientConfiguration = builder.clientOptions(clientOptions)
				.commandTimeout(Duration.ofMillis(getReadTimeout())).build();
		LettuceConnectionFactory redisConnectionFactory;
		int database = getDatabase();
		String password = getPassword();
		if (getSentinels() != null) {
			RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration(getMaster(),
					getSentinels());
			sentinelConfiguration.setDatabase(database);
			if (StringUtils.isNotBlank(password))
				sentinelConfiguration.setPassword(RedisPassword.of(password));
			redisConnectionFactory = new LettuceConnectionFactory(sentinelConfiguration, clientConfiguration);
		} else if (getClusterNodes() != null) {
			RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(getClusterNodes());
			if (StringUtils.isNotBlank(password))
				clusterConfiguration.setPassword(RedisPassword.of(password));
			redisConnectionFactory = new LettuceConnectionFactory(clusterConfiguration, clientConfiguration);
		} else {
			String hostName = getHostName();
			if (StringUtils.isNotBlank(getHost()))
				hostName = getHost();
			RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration(hostName,
					getPort());
			standaloneConfiguration.setDatabase(database);
			if (StringUtils.isNotBlank(password))
				standaloneConfiguration.setPassword(RedisPassword.of(password));
			redisConnectionFactory = new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration);
		}
		return redisConnectionFactory;
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
	public RedisMessageListenerContainer redisMessageListenerContainer(
			@Autowired(required = false) ExecutorService executorService) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory());
		if (executorService != null)
			container.setTaskExecutor(executorService);
		return container;
	}

}
