package org.ironrhino.core.coordination;

import org.ironrhino.core.coordination.ZookeeperLockServiceTests.ZooKeeperLockServiceConfiguration;
import org.ironrhino.core.coordination.impl.ZooKeeperLockService;
import org.ironrhino.core.zookeeper.ZooKeeperConfiguration;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ZooKeeperLockServiceConfiguration.class)
public class ZookeeperLockServiceTests extends LockServiceTestBase {

	@Configuration
	static class ZooKeeperLockServiceConfiguration extends ZooKeeperConfiguration {

		@Bean
		public LockService lockService() {
			return new ZooKeeperLockService();
		}
	}

}
