package org.ironrhino.core.cache;

import org.ironrhino.core.cache.impl.RedisCacheManager;
import org.ironrhino.core.spring.configuration.RedisConfiguration;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = RedisCacheManagerTests.Config.class)
public class RedisCacheManagerTests extends CacheManagerTestBase {

	@Configuration
	static class Config extends RedisConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new RedisCacheManager();
		}

	}
}
