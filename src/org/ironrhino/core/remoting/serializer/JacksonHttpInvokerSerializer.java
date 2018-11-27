package org.ironrhino.core.remoting.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JacksonHttpInvokerSerializer implements HttpInvokerSerializer {

	private static final String SEPARATOR = "|";

	private final ObjectMapper objectMapper;

	public JacksonHttpInvokerSerializer(JsonFactory jsonFactory) {
		objectMapper = JsonSerializationUtils.createNewObjectMapper(jsonFactory)
				.enableDefaultTyping(ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT, JsonTypeInfo.As.PROPERTY);
	}

	@Override
	public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return new MethodRemoteInvocation(methodInvocation);
	}

	@Override
	public void writeRemoteInvocation(RemoteInvocation remoteInvocation, OutputStream os) throws IOException {
		MethodRemoteInvocation invocation = (MethodRemoteInvocation) remoteInvocation;
		String methodName = invocation.getMethodName();
		byte[] bytes = methodName.getBytes(StandardCharsets.UTF_8);
		os.write(bytes.length);
		os.write(bytes);
		Method method = invocation.getMethod();
		String returnType = toCanonical(method.getGenericReturnType());
		bytes = returnType.getBytes(StandardCharsets.UTF_8);
		os.write(bytes.length);
		os.write(bytes);
		Type[] types = method.getGenericParameterTypes();
		String[] genericParameterTypes = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			Type type = types[i];
			genericParameterTypes[i] = toCanonical(type);
		}
		Object[] arguments = invocation.getArguments();
		ObjectNode on = new ObjectNode(objectMapper.getNodeFactory());
		for (int i = 0; i < types.length; i++) {
			String type = genericParameterTypes[i];
			Object argument = arguments[i];
			String concreteType = toConcrete(type, argument);
			on.putPOJO(concreteType.equals(type) ? type : type + SEPARATOR + concreteType, argument);
		}
		objectMapper.writeValue(os, on);
	}

	@Override
	public RemoteInvocation readRemoteInvocation(InputStream is) throws IOException {
		MethodRemoteInvocation invocation = new MethodRemoteInvocation();
		int length = is.read();
		byte[] bytes = new byte[length];
		is.read(bytes);
		invocation.setMethodName(new String(bytes, StandardCharsets.UTF_8));
		length = is.read();
		bytes = new byte[length];
		is.read(bytes);
		try {
			ObjectNode on = objectMapper.readValue(is, ObjectNode.class);
			List<Class<?>> parameterTypes = new ArrayList<>();
			List<Object> arguments = new ArrayList<>();
			Iterator<String> names = on.fieldNames();
			while (names.hasNext()) {
				String name = names.next();
				int index = name.indexOf(SEPARATOR);
				String type = index > 0 ? name.substring(0, index) : name;
				JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(type);
				parameterTypes.add(jt.getRawClass());
				if (index > 0)
					jt = objectMapper.getTypeFactory()
							.constructFromCanonical(name.substring(index + SEPARATOR.length()));
				arguments.add(objectMapper.readValue(objectMapper.treeAsTokens(on.get(type)), jt));
			}
			invocation.setParameterTypes(parameterTypes.toArray(new Class[parameterTypes.size()]));
			invocation.setArguments(arguments.toArray(new Object[arguments.size()]));
			return invocation;
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation remoteInvocation, RemoteInvocationResult result,
			OutputStream os) throws IOException {
		MethodRemoteInvocation invocation = (MethodRemoteInvocation) remoteInvocation;
		Throwable exception = result.getException();
		if (exception == null) {
			os.write(0);
			String returnType = toCanonical(invocation.getMethod().getGenericReturnType());
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
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type[] argTypes = pt.getActualTypeArguments();
			if (rawType instanceof Class && argTypes.length == 1 && argTypes[0] instanceof Class
					&& ((Class<?>) argTypes[0]).isArray()) {
				// https://github.com/FasterXML/jackson-databind/issues/2095
				return ((Class<?>) rawType).getName() + "<java.lang.Object>";
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
			} else if (jt.isMapLikeType() && (!jt.getKeyType().isConcrete() || !jt.getContentType().isConcrete())
					&& argument instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) argument;
				if (!map.isEmpty()) {
					Map.Entry<?, ?> entry = map.entrySet().iterator().next();
					Object key = entry.getKey();
					Object value = entry.getValue();
					if (key != null && value != null) {
						JavaType newJt = objectMapper.getTypeFactory().constructParametricType(jt.getRawClass(),
								objectMapper.getTypeFactory().constructType(key.getClass()),
								objectMapper.getTypeFactory().constructType(value.getClass()));
						return newJt.toCanonical();
					}
				}
			}
		}
		return type;
	}

}
