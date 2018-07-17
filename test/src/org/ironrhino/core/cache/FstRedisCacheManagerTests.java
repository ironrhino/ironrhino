package org.ironrhino.core.cache;

import org.ironrhino.core.cache.impl.RedisCacheManager;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { RedisCacheManager.SERIALIZERS_PREFIX
		+ "test\\:test=org.ironrhino.core.spring.data.redis.FstRedisSerializer" })
public class FstRedisCacheManagerTests extends RedisCacheManagerTests {

}
