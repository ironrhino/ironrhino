package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ironrhino.core.model.NullObject;
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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonSerializationUtils {

	private static final ObjectMapper defaultTypingObjectMapper = createNewObjectMapper();
	static {
		defaultTypingObjectMapper.activateDefaultTyping(defaultTypingObjectMapper.getPolymorphicTypeValidator(),
				ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
	};

	public static String serialize(Object object) throws IOException {
		if (object == null)
			return null;
		if (object instanceof Long)
			return object + "L";
		else if (object instanceof Float)
			return object + "F";
		else if (object instanceof Enum)
			return object.getClass().getName() + '.' + ((Enum<?>) object).name();
		return defaultTypingObjectMapper.writeValueAsString(object);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object deserialize(String string) throws IOException {
		if (string == null)
			return null;
		if (Character.isDigit(string.charAt(0))) {
			if (string.endsWith("L"))
				return Long.valueOf(string.substring(0, string.length() - 1));
			else if (string.endsWith("F"))
				return Float.valueOf(string.substring(0, string.length() - 1));
		}
		if (Character.isAlphabetic(string.charAt(0)) && Character.isAlphabetic(string.charAt(string.length() - 1))) {
			int index = string.lastIndexOf('.');
			if (index > 0) {
				String clazz = string.substring(0, index);
				String name = string.substring(index + 1);
				try {
					return Enum.valueOf((Class<Enum>) Class.forName(clazz), name);
				} catch (ClassNotFoundException e) {
					return null;
				}
			}
		}
		return defaultTypingObjectMapper.readValue(string, Object.class);
	}

	public static ObjectMapper createNewObjectMapper() {
		return createNewObjectMapper(null);
	}

	public static ObjectMapper createNewObjectMapper(JsonFactory jsonFactory) {
		ObjectMapper objectMapper = new ObjectMapper(jsonFactory)
				.setSerializationInclusion(JsonInclude.Include.NON_NULL)
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).addMixIn(Throwable.class, ThrowableMixin.class)
				.addMixIn(GrantedAuthority.class, SimpleGrantedAuthorityMixin.class)
				.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityMixin.class).registerModule(
						new SimpleModule().addDeserializer(NullObject.class, new JsonDeserializer<NullObject>() {
							@Override
							public NullObject deserialize(JsonParser jsonparser,
									DeserializationContext deserializationcontext)
									throws IOException, JsonProcessingException {
								return NullObject.get();
							}
						}))
				.setAnnotationIntrospector(SmartJacksonAnnotationIntrospector.INSTANCE);
		objectMapper.findAndRegisterModules();
		return objectMapper;
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
	@JsonIgnoreProperties(value = { "localizedMessage", "cause", "suppressed" }, ignoreUnknown = true)
	public static abstract class ThrowableMixin {
		@JsonCreator
		public ThrowableMixin(@JsonProperty("message") String message) {
		}
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
					if (name.startsWith("get") || name.startsWith("is")) {
						name = name.startsWith("get") ? 's' + name.substring(1) : "set" + name.substring(2);
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
				return Modifier.isTransient(modifier) || Modifier.isStatic(modifier);
			});
		}

	}

}
