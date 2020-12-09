package org.ironrhino.core.coordination;

import org.ironrhino.core.coordination.impl.RedisLockService;
import org.ironrhino.core.spring.configuration.RedisConfiguration;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisLockServiceTests.Config.class)
public class RedisLockServiceTests extends LockServiceTestBase {

	@Configuration
	static class Config extends RedisConfiguration {

		@Bean
		public LockService lockService() {
			return new RedisLockService();
		}

	}

}
