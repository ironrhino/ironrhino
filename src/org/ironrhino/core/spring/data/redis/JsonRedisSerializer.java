package org.ironrhino.core.spring.data.redis;

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
			return JsonSerializationUtils.serialize(object);
		} catch (Exception e) {
			throw new SerializationException("Cannot serialize", e);
		}
	}

	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0)
			return null;
		try {
			return (T) JsonSerializationUtils.deserialize(bytes);
		} catch (Exception e) {
			throw new SerializationException("Cannot deserialize", e);
		}
	}

}
