package org.ironrhino.core.spring.data.redis;

import java.io.Serializable;

import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

@Component("valueSerializer")
@ApplicationContextPropertiesConditional(key = "redisTemplate.useFstSerialization", value = "true")
@BeanPresentConditional("redisTemplate")
public class FstRedisSerializer implements RedisSerializer<Serializable> {

	private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	@Override
	public byte[] serialize(Serializable object) throws SerializationException {
		return conf.asByteArray(object);
	}

	@Override
	public Serializable deserialize(byte[] bytes) throws SerializationException {
		return (Serializable) conf.asObject(bytes);
	}

}
