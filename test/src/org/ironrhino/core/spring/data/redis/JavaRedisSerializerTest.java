package org.ironrhino.core.spring.data.redis;

import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

public class JavaRedisSerializerTest extends RedisSerializerTestBase {

	@Override
	protected RedisSerializer<Object> getRedisSerializer() {
		return new JdkSerializationRedisSerializer();
	}

}
