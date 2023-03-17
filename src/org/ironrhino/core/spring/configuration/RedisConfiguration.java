package org.ironrhino.core.spring.configuration;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.CLUSTER;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConfiguration.WithDatabaseIndex;
import org.springframework.data.redis.connection.RedisConfiguration.WithPassword;
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
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.EpollProvider;
import io.lettuce.core.resource.NettyCustomizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.epoll.EpollChannelOption;
import io.opentracing.contrib.redis.common.TracingConfiguration;
import io.opentracing.contrib.redis.spring.data2.connection.TracingRedisConnectionFactory;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import lombok.Getter;
import lombok.Setter;

@Order(0)
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

	@Value("${redis.connectTimeout:5000}")
	private int connectTimeout = 5000;

	@Value("${redis.readTimeout:5000}")
	private int readTimeout = 5000;

	@Value("${redis.shutdownTimeout:100}")
	private int shutdownTimeout = 100;

	@Value("${redis.useSsl:false}")
	private boolean useSsl;

	@Value("${redis.shareNativeConnection:true}")
	private boolean shareNativeConnection;

	@Bean(destroyMethod = "shutdown")
	@Primary
	public ClientResources clientResources() {
		DefaultClientResources.Builder builder = DefaultClientResources.builder();
		builder.nettyCustomizer(new NettyCustomizer() {
			@Override
			public void afterBootstrapInitialized(Bootstrap bootstrap) {
				if (EpollProvider.isAvailable()) {
					// KEEPALIVE_TIME = TCP_KEEPIDLE + TCP_KEEPINTVL * TCP_KEEPCNT = 15s +5s*3
					// TCP_USER_TIMEOUT >= KEEPALIVE_TIME
					// https://blog.cloudflare.com/when-tcp-sockets-refuse-to-die/
					bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, 15);
					bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, 5);
					bootstrap.option(EpollChannelOption.TCP_KEEPCNT, 3);
					bootstrap.option(EpollChannelOption.TCP_USER_TIMEOUT, 30000);
				}
			}
		});
		return builder.build();
	}

	@Bean
	@Primary
	public RedisConnectionFactory redisConnectionFactory() {
		ClientOptions clientOptions = ClientOptions.builder()
				.socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(getConnectTimeout())).build())
				.timeoutOptions(TimeoutOptions.enabled()).build();
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
				.clientResources(clientResources()).clientName(AppInfo.getInstanceId(true))
				.commandTimeout(Duration.ofMillis(getReadTimeout()))
				.shutdownTimeout(Duration.ofMillis(getShutdownTimeout())).build();
		org.springframework.data.redis.connection.RedisConfiguration redisConfiguration;
		if (getSentinels() != null) {
			redisConfiguration = new RedisSentinelConfiguration(getMaster(), getSentinels());
		} else if (getClusterNodes() != null) {
			redisConfiguration = new RedisClusterConfiguration(getClusterNodes());
		} else {
			redisConfiguration = new RedisStandaloneConfiguration(hostName(), getPort());
		}
		if (StringUtils.isNotBlank(password) && redisConfiguration instanceof WithPassword) {
			((WithPassword) redisConfiguration).setPassword(RedisPassword.of(getPassword()));
		}
		if (redisConfiguration instanceof WithDatabaseIndex) {
			((WithDatabaseIndex) redisConfiguration).setDatabase(getDatabase());
		}
		LettuceConnectionFactory redisConnectionFactory = new LettuceConnectionFactory(redisConfiguration,
				clientConfiguration);
		redisConnectionFactory.setShareNativeConnection(isShareNativeConnection());
		return redisConnectionFactory;
	}

	@Bean
	@Primary
	public RedisTemplate<String, ?> redisTemplate() {
		RedisTemplate<String, ?> template = new RedisTemplate<>();
		template.setConnectionFactory(wrap(redisConnectionFactory()));
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringSerializer);
		return template;
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate() {
		StringRedisTemplate template = new StringRedisTemplate();
		template.setConnectionFactory(wrap(redisConnectionFactory()));
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

	protected String hostName() {
		String hostName = getHostName();
		if (StringUtils.isNotBlank(getHost()))
			hostName = getHost();
		return hostName;
	}

	protected RedisConnectionFactory wrap(RedisConnectionFactory redisConnectionFactory) {
		if (!Tracing.isEnabled())
			return redisConnectionFactory;
		TracingConfiguration.Builder builder = new TracingConfiguration.Builder(GlobalTracer.get())
				.traceWithActiveSpanOnly(true).extensionTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
		StringBuilder service = new StringBuilder("redis");
		if (getSentinels() != null) {
			builder.extensionTag("peer.address", String.join(",", getSentinels()));
			service.append("-sentinel");
		} else if (getClusterNodes() != null) {
			builder.extensionTag("peer.address", String.join(",", getClusterNodes()));
			service.append("-cluster");
		} else {
			if (isUseSsl())
				service.append("s");
			service.append("://").append(hostName());
			if (getPort() != 6379)
				service.append(":").append(getPort());
			if (getDatabase() > 0)
				service.append("/").append(getDatabase());
		}
		builder.extensionTag(Tags.PEER_SERVICE.getKey(), service.toString());
		return new TracingRedisConnectionFactory(redisConnectionFactory, builder.build());
	}

}
