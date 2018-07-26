package org.ironrhino.core.zookeeper;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Role;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@Profile({ CLUSTER, "zookeeper" })
@Getter
@Setter
@ConfigurationProperties(prefix = "zookeeper")
public class ZooKeeperConfiguration {

	private String connectString = "localhost:2181";

	private int connectionTimeout = 10000;

	private int sessionTimeout = 60000;

	@Autowired(required = false)
	private List<ConnectionStateListener> connectionStateListeners;

	@Autowired(required = false)
	private List<UnhandledErrorListener> unhandledErrorListeners;

	@Primary
	@Bean(initMethod = "start", destroyMethod = "close")
	public CuratorFramework curatorFramework() {
		CuratorFramework curatorFramework = CuratorFrameworkFactory.builder().connectString(connectString)
				.retryPolicy(new ExponentialBackoffRetry(1000, 3)).connectionTimeoutMs(connectionTimeout)
				.sessionTimeoutMs(sessionTimeout).build();
		return curatorFramework;
	}

	@Bean
	public DefaultWatcher defaultWatcher(CuratorFramework curatorFramework) {
		DefaultWatcher defaultWatcher = new DefaultWatcher();
		curatorFramework.getCuratorListenable().addListener(defaultWatcher);
		if (connectionStateListeners != null)
			for (ConnectionStateListener listener : connectionStateListeners)
				curatorFramework.getConnectionStateListenable().addListener(listener);
		if (unhandledErrorListeners != null)
			for (UnhandledErrorListener listener : unhandledErrorListeners)
				curatorFramework.getUnhandledErrorListenable().addListener(listener);
		return defaultWatcher;
	}

}
