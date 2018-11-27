package org.ironrhino.core.remoting.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public abstract class JacksonHttpInvokerSerializer implements HttpInvokerSerializer {

	private final ObjectMapper objectMapper;

	public JacksonHttpInvokerSerializer(JsonFactory jsonFactory) {
		objectMapper = JsonSerializationUtils.createNewObjectMapper(jsonFactory)
				.registerModule(new SimpleModule().addSerializer(NullObject.class, new JsonSerializer<NullObject>() {
					@Override
					public void serialize(NullObject nullObject, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeNull();
					}
				})).disableDefaultTyping();
	}

	@Override
	public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return new JsonRpcRemoteInvocation(methodInvocation, CodecUtils.nextId());
	}

	@Override
	public void writeRemoteInvocation(RemoteInvocation remoteInvocation, OutputStream os) throws IOException {
		JsonRpcRemoteInvocation invocation = (JsonRpcRemoteInvocation) remoteInvocation;
		Request request = new Request();
		request.setId(invocation.getId());
		request.setMethod(invocation.getMethodName());
		request.setParams(invocation.getArguments());
		objectMapper.writeValue(os, request);
	}

	@Override
	public RemoteInvocation readRemoteInvocation(Class<?> serviceInterface, InputStream is) throws IOException {
		JsonRpcRemoteInvocation invocation = new JsonRpcRemoteInvocation();
		JsonNode tree;
		try {
			tree = objectMapper.readTree(is);
		} catch (JsonParseException e) {
			throw new JsonrpcException(-32700, e.getMessage(), NullObject.get());
		}
		Serializable id = null;
		JsonNode idNode = tree.get("id");
		if (idNode != null)
			id = idNode.isNumber() ? idNode.asLong() : idNode.isTextual() ? idNode.asText() : NullObject.get();
		if (!isValid(tree))
			throw new JsonrpcException(-32600, id != null ? id : NullObject.get());
		invocation.setId(id);
		invocation.setMethodName(tree.get("method").asText());
		Class<?>[] parameterTypes = null;
		Object[] arguments = null;
		if (tree.has("params")) {
			JsonNode paramsNode = tree.get("params");
			int parameterCount = paramsNode.size();
			arguments = new Object[parameterCount];
			JsonProcessingException ex = null;
			boolean methodNameExists = false;
			loop: for (Method m : serviceInterface.getMethods()) {
				boolean b = m.getName().equals(invocation.getMethodName());
				if (!methodNameExists)
					methodNameExists = b;
				if (b && m.getParameterCount() == parameterCount) {
					parameterTypes = m.getParameterTypes();
					Type[] types = m.getGenericParameterTypes();
					try {
						if (paramsNode.isArray()) {
							for (int i = 0; i < parameterCount; i++)
								arguments[i] = objectMapper.readValue(objectMapper.treeAsTokens(paramsNode.get(i)),
										objectMapper.constructType(types[i]));
						} else {
							String[] parameterNames = ReflectionUtils.getParameterNames(m);
							for (int i = 0; i < parameterCount; i++) {
								if (!paramsNode.has(parameterNames[i]))
									continue loop;
								JsonNode node = paramsNode.get(parameterNames[i]);
								if (node != null)
									arguments[i] = objectMapper.readValue(objectMapper.treeAsTokens(node),
											objectMapper.constructType(types[i]));
							}
						}
						invocation.setMethod(m);
						ex = null;
						break;
					} catch (JsonMappingException e) {
						ex = e;
						continue;
					}
				}
			}
			if (ex != null || methodNameExists && invocation.getMethod() == null)
				throw new JsonrpcException(-32602, id);
		} else {
			try {
				invocation.setMethod(serviceInterface.getMethod(invocation.getMethodName()));
			} catch (NoSuchMethodException e) {
			}
			parameterTypes = new Class<?>[0];
			arguments = new Object[0];
		}
		if (invocation.getMethod() == null)
			throw new JsonrpcException(-32601, id);
		invocation.setParameterTypes(parameterTypes);
		invocation.setArguments(arguments);
		return invocation;
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation remoteInvocation, RemoteInvocationResult result,
			OutputStream os) throws IOException {
		JsonRpcRemoteInvocation invocation = (JsonRpcRemoteInvocation) remoteInvocation;
		Response response = new Response();
		response.setId(invocation.getId());
		if (!result.hasException()) {
			response.setResult(result.getValue());
			objectMapper.writeValue(os, response);
		} else {
			Error error = new Error();
			Throwable exception = ((InvocationTargetException) result.getException()).getTargetException();
			error.setCode(-32603);
			error.setMessage(exception.getMessage());
			error.setData(exception.getClass().getName());
			response.setError(error);
			objectMapper.writeValue(os, response);
		}
	}

	@Override
	public RemoteInvocationResult readRemoteInvocationResult(MethodInvocation methodInvocation, InputStream is)
			throws IOException {
		RemoteInvocationResult result = new RemoteInvocationResult();
		try {
			JsonNode tree = objectMapper.readTree(is);
			Serializable id = null;
			JsonNode idNode = tree.get("id");
			if (idNode != null)
				id = idNode.isNumber() ? idNode.asLong() : idNode.asText();
			if (!tree.has("error")) {
				tree = tree.get("result");
				if (tree != null) {
					Type type = methodInvocation.getMethod().getGenericReturnType();
					if (type instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) type;
						Type rawType = pt.getRawType();
						if (rawType instanceof Class) {
							Class<?> clz = (Class<?>) rawType;
							if (clz.equals(Optional.class) || Callable.class.isAssignableFrom(clz)
									|| Future.class.isAssignableFrom(clz)) {
								type = pt.getActualTypeArguments()[0];
							}
						}
					}
					result.setValue(
							objectMapper.readValue(objectMapper.treeAsTokens(tree), objectMapper.constructType(type)));
				}
			} else {
				tree = tree.get("error");
				int code = tree.get("code").asInt();
				String message = tree.get("message").asText();
				if (code == -32700)
					throw new SerializationFailedException(message);
				Exception exception = null;
				if (tree.has("data")) {
					try {
						Class<?> clazz = ClassUtils.forName(tree.get("data").asText(), null);
						exception = (Exception) clazz.getConstructor(String.class).newInstance(message);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (exception == null)
					exception = new JsonrpcException(code, message, id);
				result.setException(new InvocationTargetException(exception));
			}
			return result;
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public boolean handleException(Exception ex, HttpServletResponse response) throws IOException {
		if (ex instanceof JsonrpcException) {
			JsonrpcException e = (JsonrpcException) ex;
			Response rsp = new Response();
			rsp.setId(e.getId());
			Error error = new Error();
			error.setCode(e.getCode());
			if (error.getMessage() == null)
				error.setMessage(e.getMessage());
			rsp.setError(error);
			response.setContentType(getContentType());
			objectMapper.writeValue(response.getOutputStream(), rsp);
			return true;
		}
		return false;
	}

	protected boolean isValid(JsonNode tree) {
		if (!tree.isObject())
			return false;
		ObjectNode on = (ObjectNode) tree;
		Iterator<String> names = on.fieldNames();
		while (names.hasNext()) {
			String name = names.next();
			if (!(name.equals("jsonrpc") || name.equals("method") || name.equals("params") || name.equals("id")))
				return false;
		}
		JsonNode jsonrpc = on.get("jsonrpc");
		if (jsonrpc == null || !jsonrpc.isTextual() || !jsonrpc.asText().equals(Message.VERSION))
			return false;
		JsonNode method = on.get("method");
		if (method == null || !method.isTextual() || method.asText().startsWith("rpc."))
			return false;
		JsonNode params = on.get("params");
		if (params != null && !params.isContainerNode())
			return false;
		JsonNode id = on.get("id");
		if (id != null && !(id.isNumber() || id.isTextual()))
			return false;
		return true;
	}

	@Getter
	@Setter
	private static class Message {

		public static final String VERSION = "2.0";

		private String jsonrpc = VERSION;

	}

	@Getter
	@Setter
	private static class Request extends Message {

		private String method;

		private Object[] params;

		private Serializable id;

	}

	@Getter
	@Setter
	private static class Response extends Message {

		private Object result;

		private Error error;

		private Serializable id;

	}

	@Getter
	@Setter
	private static class Error {

		private static final Map<Integer, String> PREDEFINED_ERRORS;

		static {
			Map<Integer, String> map = new HashMap<>(8);
			map.put(-32700, "Parse error");
			map.put(-32600, "Invalid Request");
			map.put(-32601, "Method not found");
			map.put(-32602, "Invalid params");
			map.put(-32603, "Internal error");
			PREDEFINED_ERRORS = Collections.unmodifiableMap(map);
		}

		private int code;

		private String message;

		private Object data;

		public void setCode(int code) {
			this.code = code;
			this.message = PREDEFINED_ERRORS.get(code);
		}

	}

	@NoArgsConstructor
	@Getter
	@Setter
	private class JsonRpcRemoteInvocation extends RemoteInvocation {

		private static final long serialVersionUID = -2740913342844528055L;

		private transient Method method;

		private Serializable id;

		JsonRpcRemoteInvocation(MethodInvocation methodInvocation, Serializable id) {
			super(methodInvocation);
			this.method = methodInvocation.getMethod();
			this.id = id;
		}

		@Override
		public Object invoke(Object targetObject)
				throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
			if (method == null)
				method = ClassUtils.getInterfaceMethodIfPossible(
						targetObject.getClass().getMethod(getMethodName(), getParameterTypes()));
			return method.invoke(targetObject, getArguments());
		}

	}

	@Getter
	static class JsonrpcException extends RuntimeException {

		private static final long serialVersionUID = -7532491242290991258L;

		private int code;

		private String message;

		private Serializable id;

		public JsonrpcException(int code) {
			this.code = code;
		}

		public JsonrpcException(int code, Serializable id) {
			this.code = code;
			this.id = id;
		}

		public JsonrpcException(int code, String message) {
			this.code = code;
			this.message = message;
		}

		public JsonrpcException(int code, String message, Serializable id) {
			this.code = code;
			this.message = message;
			this.id = id;
		}
	}

}
