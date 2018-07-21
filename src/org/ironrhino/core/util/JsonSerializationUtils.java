package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson2.SimpleGrantedAuthorityMixin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
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

public abstract class JsonSerializationUtils {

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
	@JsonIgnoreProperties(value = { "localizedMessage", "cause", "suppressed" }, ignoreUnknown = true)
	public static abstract class ThrowableMixin {
		@JsonCreator
		public ThrowableMixin(@JsonProperty("message") String message) {
		}
	}

	public static ObjectMapper createNewObjectMapper() {
		return createNewObjectMapper(null);
	}

	public static ObjectMapper createNewObjectMapper(JsonFactory jsonFactory) {
		return new ObjectMapper(jsonFactory)
				.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).addMixIn(Throwable.class, ThrowableMixin.class)
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
				})).setAnnotationIntrospector(SmartJacksonAnnotationIntrospector.INSTANCE);
	}

	static class SmartJacksonAnnotationIntrospector extends JacksonAnnotationIntrospector {

		public static final SmartJacksonAnnotationIntrospector INSTANCE = new SmartJacksonAnnotationIntrospector();

		private static final long serialVersionUID = 412899061519525960L;

		private final Map<Member, Boolean> cache = new ConcurrentHashMap<>(1024);

		private SmartJacksonAnnotationIntrospector() {

		}

		@Override
		public boolean hasIgnoreMarker(AnnotatedMember m) {
			Member member = m.getMember();
			Class<?> declaringClass = member.getDeclaringClass();
			if (GrantedAuthority.class.isAssignableFrom(declaringClass) || declaringClass.getName().startsWith("java."))
				return false;
			return cache.computeIfAbsent(member, mem -> {
				if (mem instanceof Method) {
					Method method = (Method) mem;
					String name = method.getName();
					if (name.startsWith("get")) {
						name = 's' + name.substring(1);
						try {
							declaringClass.getMethod(name, method.getReturnType());
							return false;
						} catch (NoSuchMethodException e) {
							boolean hasOtherSetter = false;
							for (Method met : declaringClass.getMethods()) {
								if (met.getName().startsWith("set") && met.getReturnType() == void.class
										&& met.getParameterTypes().length == 1) {
									hasOtherSetter = true;
									break;
								}
							}
							return hasOtherSetter;
						}
					}
				}
				int modifier = mem.getModifiers();
				return Modifier.isTransient(modifier) || Modifier.isFinal(modifier) || Modifier.isStatic(modifier);
			});
		}

	}

}
