package org.ironrhino.core.spring.data.redis;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

import org.ironrhino.core.model.NullObject;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson2.SimpleGrantedAuthorityMixin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;

@SuppressWarnings("unchecked")
public class JsonRedisSerializer<T> implements RedisSerializer<T> {

	private final ObjectMapper objectMapper;

	public JsonRedisSerializer() {
		objectMapper = new ObjectMapper()
				.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
				.addMixIn(GrantedAuthority.class, SimpleGrantedAuthorityMixin.class)
				.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityMixin.class)
				.registerModule(new SimpleModule().addDeserializer(LocalDate.class, new JsonDeserializer<LocalDate>() {
					@Override
					public LocalDate deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
							throws IOException, JsonProcessingException {
						return LocalDate.parse(jsonparser.getText());
					}
				}).addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
					@Override
					public void serialize(LocalDate localDate, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeString(localDate.toString());
					}
				}).addDeserializer(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
					@Override
					public LocalDateTime deserialize(JsonParser jsonparser,
							DeserializationContext deserializationcontext) throws IOException, JsonProcessingException {
						return LocalDateTime.parse(jsonparser.getText());
					}
				}).addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
					@Override
					public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeString(localDateTime.toString());
					}
				}).addDeserializer(LocalTime.class, new JsonDeserializer<LocalTime>() {
					@Override
					public LocalTime deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
							throws IOException, JsonProcessingException {
						return LocalTime.parse(jsonparser.getText());
					}
				}).addSerializer(LocalTime.class, new JsonSerializer<LocalTime>() {
					@Override
					public void serialize(LocalTime localTime, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeString(localTime.toString());
					}
				}).addDeserializer(YearMonth.class, new JsonDeserializer<YearMonth>() {
					@Override
					public YearMonth deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
							throws IOException, JsonProcessingException {
						return YearMonth.parse(jsonparser.getText());
					}
				}).addSerializer(YearMonth.class, new JsonSerializer<YearMonth>() {
					@Override
					public void serialize(YearMonth yearMonth, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeString(yearMonth.toString());
					}
				}).addDeserializer(Duration.class, new JsonDeserializer<Duration>() {
					@Override
					public Duration deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
							throws IOException, JsonProcessingException {
						return Duration.parse(jsonparser.getText());
					}
				}).addSerializer(Duration.class, new JsonSerializer<Duration>() {
					@Override
					public void serialize(Duration duration, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeString(duration.toString());
					}
				}).addDeserializer(NullObject.class, new JsonDeserializer<NullObject>() {
					@Override
					public NullObject deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
							throws IOException, JsonProcessingException {
						return NullObject.get();
					}
				})).setAnnotationIntrospector(new JacksonAnnotationIntrospector() {

					private static final long serialVersionUID = 1L;

					@Override
					public boolean hasIgnoreMarker(final AnnotatedMember m) {
						return Modifier.isTransient(m.getMember().getModifiers());
					}

				});
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
