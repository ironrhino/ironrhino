package org.ironrhino.core.zookeeper;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * ZooKeeper应用客户端连接服务端的配置器
 */
@Configuration
@Profile(CLUSTER)
public class ZooKeeperConfiguration {

  /**
   * ZooKeeper服务端地址
   */
  @Value("${zooKeeper.connectString:localhost:2181}")
  private String connectString;

  /**
   * 设置连接超时
   */
  @Value("${zooKeeper.connectionTimeout:10000}")
  private int connectionTimeout;

  /**
   * 设置session过期
   */
  @Value("${zooKeeper.sessionTimeout:60000}")
  private int sessionTimeout;

  /**
   * 实例化curatorFramework连接器, 并加载CuratorListener默认的监听器.
   * 
   * @return curatorFramework
   */
  public @Bean(initMethod = "start", destroyMethod = "close")
  CuratorFramework curatorFramework() {
    CuratorFramework curatorFramework =
        CuratorFrameworkFactory.builder().connectString(connectString)
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .connectionTimeoutMs(connectionTimeout).sessionTimeoutMs(sessionTimeout).build();
    curatorFramework.getCuratorListenable().addListener(defaultWatcher());
    return curatorFramework;
  }

  public @Bean
  DefaultWatcher defaultWatcher() {
    return new DefaultWatcher();
  }

}
