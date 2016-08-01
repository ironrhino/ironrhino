package org.ironrhino.core.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ironrhino.core.util.JsonUtils;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonHttpInvokerSerializationHelper {

	private static final String SEPARATOR = "|";

	private static ObjectMapper objectMapper = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

	public static void writeRemoteInvocation(RemoteInvocation remoteInvocation, OutputStream os) throws IOException {
		GenericRemoteInvocation invocation = (GenericRemoteInvocation) remoteInvocation;
		String methodName = invocation.getMethodName();
		String returnType = invocation.getGenericReturnType();
		String[] types = invocation.getGenericParameterTypes();
		Object[] arguments = invocation.getArguments();
		ObjectNode on = new ObjectNode(objectMapper.getNodeFactory());
		for (int i = 0; i < types.length; i++) {
			String type = types[i];
			Object argument = arguments[i];
			JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(type);
			if (!jt.isContainerType() && !jt.isConcrete() && argument != null)
				type += SEPARATOR + objectMapper.getTypeFactory().constructType(argument.getClass()).toCanonical();
			on.putPOJO(type, argument);
		}
		byte[] bytes = methodName.getBytes(StandardCharsets.UTF_8);
		os.write(bytes.length);
		os.write(bytes);
		bytes = returnType.getBytes(StandardCharsets.UTF_8);
		os.write(bytes.length);
		os.write(bytes);
		objectMapper.writeValue(os, on);
	}

	public static RemoteInvocation readRemoteInvocation(InputStream is) throws IOException {
		GenericRemoteInvocation invocation = new GenericRemoteInvocation();
		int length = is.read();
		byte[] bytes = new byte[length];
		is.read(bytes);
		invocation.setMethodName(new String(bytes, StandardCharsets.UTF_8));
		length = is.read();
		bytes = new byte[length];
		is.read(bytes);
		invocation.setGenericReturnType(new String(bytes, StandardCharsets.UTF_8));
		JsonNode node = objectMapper.readValue(is, JsonNode.class);
		if (node instanceof ObjectNode) {
			ObjectNode on = (ObjectNode) node;
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
				arguments.add(objectMapper.readValue(on.get(type).toString(), jt));
			}
			invocation.setGenericParameterTypes(genericParameterTypes.toArray(new String[0]));
			invocation.setParameterTypes(parameterTypes.toArray(new Class[0]));
			invocation.setArguments(arguments.toArray(new Object[0]));
			return invocation;
		} else {
			throw new RemoteException(
					"Deserialized object needs to be assignable to type [" + ObjectNode.class.getName() + "]: ");
		}
	}

	public static void writeRemoteInvocationResult(RemoteInvocation remoteInvocation, RemoteInvocationResult result,
			OutputStream os) throws IOException {
		GenericRemoteInvocation invocation = (GenericRemoteInvocation) remoteInvocation;
		Throwable exception = result.getException();
		if (exception == null) {
			os.write(0);
			String returnType = invocation.getGenericReturnType();
			JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(returnType);
			if (!jt.isContainerType() && !jt.isConcrete() && result.getValue() != null)
				returnType += SEPARATOR
						+ objectMapper.getTypeFactory().constructType(result.getValue().getClass()).toCanonical();
			byte[] bytes = returnType.getBytes(StandardCharsets.UTF_8);
			os.write(bytes.length);
			os.write(bytes);
			objectMapper.writeValue(os, result.getValue());
		} else {
			exception = ((InvocationTargetException) exception).getTargetException();
			os.write(1);
			Map<String, String> map = new LinkedHashMap<>();
			map.put("type", exception.getClass().getName());
			map.put("message", exception.getMessage());
			objectMapper.writeValue(os, map);
		}
	}

	public static RemoteInvocationResult readRemoteInvocationResult(InputStream is)
			throws IOException, ClassNotFoundException {
		RemoteInvocationResult result = new RemoteInvocationResult();
		int i = is.read();
		if (i == 0) {
			int length = is.read();
			byte[] bytes = new byte[length];
			is.read(bytes);
			String name = new String(bytes, StandardCharsets.UTF_8);
			int index = name.indexOf(SEPARATOR);
			String type = index > 0 ? name.substring(index + SEPARATOR.length()) : name;
			JavaType jt = objectMapper.getTypeFactory().constructFromCanonical(type);
			Object value = jt.toCanonical().equals("void") ? null : objectMapper.readValue(is, jt);
			result.setValue(value);
			return result;
		} else if (i == 1) {
			Map<String, String> map = objectMapper.readValue(is, JsonUtils.STRING_MAP_TYPE);
			Class<?> clz = Class.forName(map.get("type"));
			Throwable throwable;
			try {
				throwable = (Throwable) clz.getConstructor(String.class).newInstance(map.get("message"));
			} catch (Exception e) {
				throw new RemoteException(e.getMessage(), e);
			}
			InvocationTargetException exception = new InvocationTargetException(throwable);
			result.setException(exception);
			return result;
		}
		throw new RemoteException("Illegal stream");
	}

	public static String toCanonical(Type type) {
		return objectMapper.getTypeFactory().constructType(type).toCanonical();
	}

}
