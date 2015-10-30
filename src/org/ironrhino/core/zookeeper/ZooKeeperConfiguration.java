package org.ironrhino.core.zookeeper;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(CLUSTER)
public class ZooKeeperConfiguration {

	@Value("${zooKeeper.connectString:localhost:2181}")
	private String connectString;

	@Value("${zooKeeper.connectionTimeout:10000}")
	private int connectionTimeout;

	@Value("${zooKeeper.sessionTimeout:60000}")
	private int sessionTimeout;

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
		return defaultWatcher;
	}

}
