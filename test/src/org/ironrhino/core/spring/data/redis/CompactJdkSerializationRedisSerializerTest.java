package org.ironrhino.core.spring.data.redis;

import org.springframework.data.redis.serializer.RedisSerializer;

public class CompactJdkSerializationRedisSerializerTest extends RedisSerializerTestBase {

	@Override
	protected RedisSerializer<Object> getRedisSerializer() {
		return new CompactJdkSerializationRedisSerializer();
	}

}
