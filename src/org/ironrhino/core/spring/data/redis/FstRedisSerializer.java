package org.ironrhino.core.spring.data.redis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.annotation.PostConstruct;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Component
@ApplicationContextPropertiesConditional(key = "redisTemplate.useFstSerialization", value = "true")
@BeanPresentConditional("redisTemplate")
public class FstRedisSerializer<T> implements RedisSerializer<T> {

	private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	@Autowired
	@Qualifier("redisTemplate")
	private RedisTemplate redisTemplate;

	@PostConstruct
	public void init() {
		redisTemplate.setValueSerializer(this);
	}

	@Override
	public byte[] serialize(T object) throws SerializationException {
		try {
			return conf.asByteArray(object);
		} catch (Exception e) {
			throw new SerializationException("Cannot serialize", e);
		}
	}

	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0)
			return null;
		try {
			return (T) conf.asObject(bytes);
		} catch (Exception e) {
			if (e instanceof IOException && e.getCause() instanceof NullPointerException
					&& org.ironrhino.core.util.StringUtils.isUtf8(bytes))
				return (T) new String(bytes, StandardCharsets.UTF_8);
			throw new SerializationException("Cannot deserialize", e);
		}
	}

}
