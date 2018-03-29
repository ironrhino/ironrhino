package org.ironrhino.core.coordination;

import org.ironrhino.core.coordination.RedisLockServiceTests.RedisLockServiceConfiguration;
import org.ironrhino.core.coordination.impl.RedisLockService;
import org.ironrhino.core.spring.configuration.RedisConfiguration;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisLockServiceConfiguration.class)
public class RedisLockServiceTests extends LockServiceTestBase {

	@Configuration
	static class RedisLockServiceConfiguration extends RedisConfiguration {

		@Bean
		public LockService lockService() {
			return new RedisLockService();
		}

	}

}
