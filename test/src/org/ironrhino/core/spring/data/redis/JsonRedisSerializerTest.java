package org.ironrhino.core.spring.data.redis;

import org.junit.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

public class JsonRedisSerializerTest extends RedisSerializerTestBase {

	@Override
	@Test
	public void testSimpleObject() {
		// JsonRedisSerializer doesn't supports this
	}

	@Override
	protected RedisSerializer<Object> getRedisSerializer() {
		return new JsonRedisSerializer<>();
	}

}
