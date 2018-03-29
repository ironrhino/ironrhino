package org.ironrhino.core.cache;

import org.ironrhino.core.cache.MemcachedCacheManagerTests.MemcachedCacheManagerConfiguration;
import org.ironrhino.core.cache.impl.MemcachedCacheManager;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MemcachedCacheManagerConfiguration.class)
public class MemcachedCacheManagerTests extends CacheManagerTestBase {

	@Configuration
	static class MemcachedCacheManagerConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new MemcachedCacheManager();
		}

	}
}
