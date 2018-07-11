package org.ironrhino.core.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public abstract class JacksonHttpInvokerSerializer implements HttpInvokerSerializer {

	private static final String SEPARATOR = "|";

	private final ObjectMapper objectMapper;

	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
	@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
	@JsonIgnoreProperties(value = { "localizedMessage", "cause", "suppressed" }, ignoreUnknown = true)
	public static abstract class ThrowableMixin {
		@JsonCreator
		public ThrowableMixin(@JsonProperty("message") String message) {
		}
	}

	public JacksonHttpInvokerSerializer(JsonFactory jsonFactory) {
		objectMapper = new ObjectMapper(jsonFactory)
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
				}));
	}

	@Override
	public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return new JacksonRemoteInvocation(methodInvocation);
	}

	@Override
	public void writeRemoteInvocation(RemoteInvocation remoteInvocation, OutputStream os) throws IOException {
		JacksonRemoteInvocation invocation = (JacksonRemoteInvocation) remoteInvocation;
		String methodName = invocation.getMethodName();
		String returnType = invocation.getGenericReturnType();
		String[] types = invocation.getGenericParameterTypes();
		Object[] arguments = invocation.getArguments();
		ObjectNode on = new ObjectNode(objectMapper.getNodeFactory());
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object argument = arguments[i];
			String concreteType = toConcrete(type, argument);
			on.putPOJO(concreteType.equals(type) ? type : type + SEPARATOR + concreteType, argument);
		}
		byte[] bytes = methodName.getBytes(StandardCharsets.UTF_8);
		os.write(bytes.length);
		os.write(bytes);
		bytes = returnType.getBytes(StandardCharsets.UTF_8);
		os.write(bytes.length);
		os.write(bytes);
		objectMapper.writeValue(os, on);
	}

	@Override
	public RemoteInvocation readRemoteInvocation(InputStream is) throws IOException {
		JacksonRemoteInvocation invocation = new JacksonRemoteInvocation();
		int length = is.read();
		byte[] bytes = new byte[length];
		is.read(bytes);
		invocation.setMethodName(new String(bytes, StandardCharsets.UTF_8));
		length = is.read();
		bytes = new byte[length];
		is.read(bytes);
		invocation.setGenericReturnType(new String(bytes, StandardCharsets.UTF_8));
		try {
			ObjectNode on = objectMapper.readValue(is, ObjectNode.class);
			List<String> genericParameterTypes = new ArrayList<>();
			List<Class<?>> parameterTypes = new ArrayList<>();
			List<Object> arguments = new ArrayList<>();
			Iterator<String> names = on.fieldNames();
			while (names.hasNext()) {
				String name = names.next();
				int index = name.indexOf(SEPARATOR);
				String type = index > 0 ? name.substring(0, index) : name;
				genericParameterTypes.add(type);
				JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(type);
				parameterTypes.add(jt.getRawClass());
				if (index > 0)
					jt = objectMapper.getTypeFactory()
							.constructFromCanonical(name.substring(index + SEPARATOR.length()));
				arguments.add(objectMapper.readValue(objectMapper.treeAsTokens(on.get(type)), jt));
			}
			invocation.setGenericParameterTypes(genericParameterTypes.toArray(new String[0]));
			invocation.setParameterTypes(parameterTypes.toArray(new Class[0]));
			invocation.setArguments(arguments.toArray(new Object[0]));
			return invocation;
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation remoteInvocation, RemoteInvocationResult result,
			OutputStream os) throws IOException {
		JacksonRemoteInvocation invocation = (JacksonRemoteInvocation) remoteInvocation;
		Throwable exception = result.getException();
		if (exception == null) {
			os.write(0);
			String returnType = invocation.getGenericReturnType();
			returnType = toConcrete(returnType, result.getValue());
			byte[] bytes = returnType.getBytes(StandardCharsets.UTF_8);
			os.write(bytes.length);
			os.write(bytes);
			objectMapper.writeValue(os, result.getValue());
		} else {
			exception = ((InvocationTargetException) exception).getTargetException();
			os.write(1);
			objectMapper.writeValue(os, exception);
		}
	}

	@Override
	public RemoteInvocationResult readRemoteInvocationResult(InputStream is) throws IOException {
		RemoteInvocationResult result = new RemoteInvocationResult();
		int i = is.read();
		try {
			if (i == 0) {
				int length = is.read();
				byte[] bytes = new byte[length];
				is.read(bytes);
				String type = new String(bytes, StandardCharsets.UTF_8);
				if (!type.equals("void")) {
					JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(type);
					result.setValue(objectMapper.readValue(is, jt));
				}
				return result;
			} else {
				Throwable throwable = objectMapper.readValue(is, Throwable.class);
				InvocationTargetException exception = new InvocationTargetException(throwable);
				result.setException(exception);
				return result;
			}
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	protected String toCanonical(Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			if (rawType.equals(Optional.class)) {
				type = pt.getActualTypeArguments()[0];
			}
		}
		return objectMapper.getTypeFactory().constructType(type).toCanonical();
	}

	protected String toConcrete(String type, Object argument) {
		JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(type);
		if (argument != null) {
			if (!jt.isContainerType() && !jt.isConcrete()) {
				return objectMapper.getTypeFactory().constructType(argument.getClass()).toCanonical();
			} else if (jt.isCollectionLikeType() && !jt.getContentType().isConcrete()
					&& argument instanceof Collection) {
				Collection<?> coll = (Collection<?>) argument;
				if (!coll.isEmpty()) {
					JavaType newJt = objectMapper.getTypeFactory().constructParametricType(jt.getRawClass(),
							objectMapper.getTypeFactory().constructType(coll.iterator().next().getClass()));
					return newJt.toCanonical();
				}
			}
		}
		return type;
	}

	@NoArgsConstructor
	private class JacksonRemoteInvocation extends RemoteInvocation {

		private static final long serialVersionUID = -2740913342844528055L;

		@Getter
		@Setter
		private String[] genericParameterTypes;

		@Getter
		@Setter
		private String genericReturnType;

		JacksonRemoteInvocation(MethodInvocation methodInvocation) {
			super(methodInvocation);
			Type[] types = methodInvocation.getMethod().getGenericParameterTypes();
			genericParameterTypes = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				Type type = types[i];
				genericParameterTypes[i] = toCanonical(type);
			}
			Type type = methodInvocation.getMethod().getGenericReturnType();
			genericReturnType = toCanonical(type);
		}

	}

}
