package org.ironrhino.core.spring.data.redis;

import java.nio.charset.StandardCharsets;

import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class FstRedisSerializer<T> implements RedisSerializer<T> {

	private final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	@Override
	public byte[] serialize(T object) throws SerializationException {
		try {
			return conf.asByteArray(object);
		} catch (Exception e) {
			throw new SerializationException("Cannot serialize", e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0)
			return null;
		try {
			return (T) conf.asObject(bytes);
		} catch (Exception e) {
			if (e.getCause() instanceof NullPointerException && org.ironrhino.core.util.StringUtils.isUtf8(bytes))
				return (T) new String(bytes, StandardCharsets.UTF_8);
			throw new SerializationException("Cannot deserialize", e);
		}
	}

}
