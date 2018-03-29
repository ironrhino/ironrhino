package org.ironrhino.core.cache;

import org.ironrhino.core.cache.Cache2kCacheManagerTest.Cache2kCacheManagerConfiguration;
import org.ironrhino.core.cache.impl.Cache2kCacheManager;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = Cache2kCacheManagerConfiguration.class)
public class Cache2kCacheManagerTest extends CacheManagerTestBase {

	@Configuration
	static class Cache2kCacheManagerConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new Cache2kCacheManager();
		}

	}
}
