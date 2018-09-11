package org.ironrhino.core.spring.data.redis;

import java.nio.charset.StandardCharsets;

import org.ironrhino.core.util.JsonSerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

@SuppressWarnings("unchecked")
public class JsonRedisSerializer<T> implements RedisSerializer<T> {

	@Override
	public byte[] serialize(T object) throws SerializationException {
		try {
			if (object == null)
				return new byte[0];
			return JsonSerializationUtils.serialize(object).getBytes(StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new SerializationException("Cannot serialize", e);
		}
	}

	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0)
			return null;
		String string = new String(bytes, StandardCharsets.UTF_8);

		try {
			return (T) JsonSerializationUtils.deserialize(string);
		} catch (Exception e) {
			throw new SerializationException("Cannot deserialize", e);
		}
	}

}
