package org.ironrhino.core.spring.data.redis;

import java.io.IOException;

import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("unchecked")
public class JsonRedisSerializer<T> implements RedisSerializer<T> {

	private final ObjectMapper objectMapper;

	public JsonRedisSerializer() {
		objectMapper = JsonSerializationUtils.createNewObjectMapper()
				.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY).registerModule(
						new SimpleModule().addDeserializer(NullObject.class, new JsonDeserializer<NullObject>() {
							@Override
							public NullObject deserialize(JsonParser jsonparser,
									DeserializationContext deserializationcontext)
									throws IOException, JsonProcessingException {
								return NullObject.get();
							}
						}));
	}

	@Override
	public byte[] serialize(T object) throws SerializationException {
		try {
			if (object == null)
				return new byte[0];
			byte[] bytes = objectMapper.writeValueAsBytes(object);
			return bytes;
		} catch (Exception e) {
			throw new SerializationException("Cannot serialize", e);
		}
	}

	@Override
	public T deserialize(byte[] bytes) throws SerializationException {
		if (bytes == null || bytes.length == 0)
			return null;
		try {
			return (T) objectMapper.readValue(bytes, Object.class);
		} catch (Exception e) {
			throw new SerializationException("Cannot deserialize", e);
		}
	}

}
