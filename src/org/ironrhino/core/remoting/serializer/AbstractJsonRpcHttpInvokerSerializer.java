package org.ironrhino.core.remoting.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.slf4j.MDC;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.ironrhino.core.util.GenericTypeResolver;
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
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractJsonRpcHttpInvokerSerializer implements HttpInvokerSerializer {

	private final static String VERSION = "2.0";
	private final static String JSONRPC = "jsonrpc";
	private final static String METHOD = "method";
	private final static String PARAMS = "params";
	private final static String ID = "id";
	private final static String RESULT = "result";
	private final static String ERROR = "error";
	private final static String CODE = "code";
	private final static String MESSAGE = "message";
	private final static String DATA = "data";
	private final static int CODE_PARSE_ERROR = -32700;
	private final static int CODE_INVALID_REQUEST = -32600;
	private final static int CODE_METHOD_NOT_FOUND = -32601;
	private final static int CODE_INVALID_PARAMS = -32602;
	private final static int CODE_INTERNAL_ERROR = -32603;
	private static final Map<Integer, String> PREDEFINED_ERRORS;
	static {
		Map<Integer, String> map = new HashMap<>(8);
		map.put(CODE_PARSE_ERROR, "Parse error");
		map.put(CODE_INVALID_REQUEST, "Invalid request");
		map.put(CODE_METHOD_NOT_FOUND, "Method not found");
		map.put(CODE_INVALID_PARAMS, "Invalid params");
		map.put(CODE_INTERNAL_ERROR, "Internal error");
		PREDEFINED_ERRORS = Collections.unmodifiableMap(map);
	}

	private final ObjectMapper objectMapper;

	public AbstractJsonRpcHttpInvokerSerializer(JsonFactory jsonFactory) {
		objectMapper = JsonSerializationUtils.createNewObjectMapper(jsonFactory)
				.registerModule(new SimpleModule().addSerializer(NullObject.class, new JsonSerializer<NullObject>() {
					@Override
					public void serialize(NullObject nullObject, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeNull();
					}
				}));
	}

	@Override
	public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return new JsonrpcRemoteInvocation(methodInvocation, CodecUtils.nextId());
	}

	@Override
	public void writeRemoteInvocation(RemoteInvocation remoteInvocation, OutputStream os) throws IOException {
		JsonrpcRemoteInvocation invocation = (JsonrpcRemoteInvocation) remoteInvocation;
		Request request = new Request();
		request.setId(invocation.getId());
		request.setMethod(invocation.getMethodName());
		request.setParams(invocation.getArguments());
		objectMapper.writeValue(os, request);
	}

	@Override
	public RemoteInvocation readRemoteInvocation(Class<?> serviceInterface, InputStream is) throws IOException {
		JsonrpcRemoteInvocation invocation = new JsonrpcRemoteInvocation();
		JsonNode tree;
		try {
			tree = objectMapper.readTree(is);
			if (tree == null || tree instanceof MissingNode)
				throw new JsonRpcException(CODE_PARSE_ERROR, "", NullObject.get());
		} catch (JsonParseException e) {
			throw new JsonRpcException(CODE_PARSE_ERROR, e.getMessage(), NullObject.get());
		}
		Serializable id = null;
		JsonNode idNode = tree.get(ID);
		if (idNode != null)
			id = idNode.isNull() ? NullObject.get()
					: idNode.isNumber() ? idNode.asLong() : idNode.isTextual() ? idNode.asText() : NullObject.get();
		if (!isValid(tree))
			throw new JsonRpcException(CODE_INVALID_REQUEST, id != null ? id : NullObject.get());
		invocation.setId(id);
		invocation.setMethodName(tree.get(METHOD).asText());
		Class<?>[] parameterTypes = null;
		Object[] arguments = null;
		if (tree.has(PARAMS)) {
			JsonNode paramsNode = tree.get(PARAMS);
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
					for (int i = 0; i < types.length; i++) {
						if (types[i] instanceof TypeVariable || types[i] instanceof ParameterizedType)
							types[i] = GenericTypeResolver.resolveType(types[i], serviceInterface);
					}
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
				throw new JsonRpcException(CODE_INVALID_PARAMS, id);
		} else {
			try {
				invocation.setMethod(serviceInterface.getMethod(invocation.getMethodName()));
			} catch (NoSuchMethodException e) {
			}
			parameterTypes = new Class<?>[0];
			arguments = new Object[0];
		}
		if (invocation.getMethod() == null)
			throw new JsonRpcException(CODE_METHOD_NOT_FOUND, id);
		invocation.setParameterTypes(parameterTypes);
		invocation.setArguments(arguments);
		return invocation;
	}

	@Override
	public void writeRemoteInvocationResult(RemoteInvocation remoteInvocation, RemoteInvocationResult result,
			OutputStream os) throws IOException {
		JsonrpcRemoteInvocation invocation = (JsonrpcRemoteInvocation) remoteInvocation;
		Response response = new Response();
		response.setId(invocation.getId());
		if (!result.hasException()) {
			Object value = result.getValue();
			response.setResult(value == null ? NullObject.get() : value);
			objectMapper.writeValue(os, response);
		} else {
			Error error = new Error();
			InvocationTargetException ex = ((InvocationTargetException) result.getException());
			if (ex != null) {
				Throwable exception = ex.getTargetException();
				error.setCode(CODE_INTERNAL_ERROR);
				error.setMessage(exception.getMessage());
				error.setData(exception.getClass().getName());
				response.setError(error);
				objectMapper.writeValue(os, response);
			}
		}
	}

	@Override
	public RemoteInvocationResult readRemoteInvocationResult(MethodInvocation methodInvocation, InputStream is)
			throws IOException {
		RemoteInvocationResult result = new RemoteInvocationResult();
		try {
			JsonNode tree = objectMapper.readTree(is);
			if (tree == null) {
				// notification
				throw new JsonRpcException(-1, "JSON-RPC Notification do not have Response");
			}
			Serializable id = null;
			JsonNode idNode = tree.get(ID);
			if (idNode != null)
				id = idNode.isNumber() ? idNode.asLong() : idNode.asText();
			if (!tree.has(ERROR)) {
				tree = tree.get(RESULT);
				if (tree != null && !tree.isNull()) {
					Type type = methodInvocation.getMethod().getGenericReturnType();
					if ((type instanceof TypeVariable || type instanceof ParameterizedType)
							&& methodInvocation instanceof ReflectiveMethodInvocation) {
						// try resolve generic type
						ReflectiveMethodInvocation rmi = (ReflectiveMethodInvocation) methodInvocation;
						Class<?>[] interfaces = rmi.getProxy().getClass().getInterfaces();
						for (Class<?> intf : interfaces) {
							if (intf.isAnnotationPresent(Remoting.class)) {
								type = GenericTypeResolver.resolveType(type, intf);
								break;
							}
						}
					}
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
				tree = tree.get(ERROR);
				int code = tree.get(CODE).asInt();
				String message = tree.get(MESSAGE).asText();
				if (code == CODE_PARSE_ERROR)
					throw new SerializationFailedException(message);
				Exception exception = null;
				if (tree.has(DATA)) {
					try {
						Class<?> clazz = ClassUtils.forName(tree.get(DATA).asText(), null);
						exception = (Exception) clazz.getConstructor(String.class).newInstance(message);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (exception == null)
					exception = new JsonRpcException(code, message, id);
				result.setException(new InvocationTargetException(exception));
			}
			return result;
		} catch (JsonProcessingException e) {
			throw new SerializationFailedException(e.getMessage(), e);
		}
	}

	@Override
	public boolean handleException(Exception ex, HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		if (ex instanceof JsonRpcException) {
			JsonRpcException e = (JsonRpcException) ex;
			Response rsp = new Response();
			rsp.setId(e.getId());
			Error error = new Error();
			error.setCode(e.getCode());
			String message = PREDEFINED_ERRORS.get(e.getCode());
			if (message == null)
				message = e.getMessage();
			error.setMessage(message);
			rsp.setError(error);
			if (e.getCode() == CODE_PARSE_ERROR && request.getHeader(AccessFilter.HTTP_HEADER_REQUEST_ID) != null) {
				// invoked via HttpInvokerClient
				response.setStatus(RemotingContext.SC_SERIALIZATION_FAILED);
			}
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
			if (!(name.equals(JSONRPC) || name.equals(METHOD) || name.equals(PARAMS) || name.equals(ID)))
				return false;
		}
		JsonNode jsonrpc = on.get(JSONRPC);
		if (jsonrpc == null || !jsonrpc.isTextual() || !jsonrpc.asText().equals(VERSION))
			return false;
		JsonNode method = on.get(METHOD);
		if (method == null || !method.isTextual() || method.asText().startsWith("rpc."))
			return false;
		JsonNode params = on.get(PARAMS);
		if (params != null && !params.isContainerNode())
			return false;
		JsonNode id = on.get(ID);
		if (id != null && !(id.isNull() || id.isNumber() || id.isTextual()))
			return false;
		return true;
	}

	@Getter
	@Setter
	private static class Message {

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

		private int code;

		private String message;

		private Object data;

	}

	@NoArgsConstructor
	@Getter
	@Setter
	private class JsonrpcRemoteInvocation extends RemoteInvocation {

		private static final long serialVersionUID = -2740913342844528055L;

		private transient Method method;

		private Serializable id;

		JsonrpcRemoteInvocation(MethodInvocation methodInvocation, Serializable id) {
			super(methodInvocation);
			this.method = methodInvocation.getMethod();
			this.id = id;
		}

		@Override
		public Object invoke(Object targetObject)
				throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
			if (method == null)
				method = ClassUtils.getInterfaceMethodIfPossible(
						targetObject.getClass().getMethod(getMethodName(), getParameterTypes()), null);
			if (id == null) {
				// notification
				Map<String, String> contextMap = MDC.getCopyOfContextMap();
				ForkJoinPool.commonPool().execute(() -> {
					try {
						MDC.setContextMap(contextMap);
						method.invoke(targetObject, getArguments());
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				});
				return NullObject.get();
			} else {
				return method.invoke(targetObject, getArguments());
			}

		}

	}

	@Getter
	static class JsonRpcException extends RuntimeException {

		private static final long serialVersionUID = -7532491242290991258L;

		private int code;

		private String message;

		private Serializable id;

		public JsonRpcException(int code) {
			this.code = code;
		}

		public JsonRpcException(int code, Serializable id) {
			this.code = code;
			this.id = id;
		}

		public JsonRpcException(int code, String message) {
			this.code = code;
			this.message = message;
		}

		public JsonRpcException(int code, String message, Serializable id) {
			this.code = code;
			this.message = message;
			this.id = id;
		}
	}

}
