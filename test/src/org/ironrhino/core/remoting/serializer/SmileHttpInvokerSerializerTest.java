package org.ironrhino.core.remoting.serializer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.ironrhino.sample.remoting.TestService;
import org.ironrhino.sample.remoting.TestService.FutureType;
import org.ironrhino.security.domain.User;
import org.junit.Test;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.security.core.authority.AuthorityUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

public class SmileHttpInvokerSerializerTest extends JavaHttpInvokerSerializerTest {

	protected final static String VERSION = "2.0";
	protected final static String JSONRPC = "jsonrpc";
	protected final static String METHOD = "method";
	protected final static String PARAMS = "params";
	protected final static String ID = "id";
	protected final static String RESULT = "result";
	protected final static String ERROR = "error";
	protected final static String CODE = "code";
	protected final static String MESSAGE = "message";
	protected final static String DATA = "data";
	protected final static int CODE_PARSE_ERROR = -32700;
	protected final static int CODE_INVALID_REQUEST = -32600;
	protected final static int CODE_METHOD_NOT_FOUND = -32601;
	protected final static int CODE_INVALID_PARAMS = -32602;
	protected final static int CODE_INTERNAL_ERROR = -32603;

	protected ObjectMapper objectMapper;

	public SmileHttpInvokerSerializerTest() {
		this.objectMapper = JsonSerializationUtils.createNewObjectMapper(new SmileFactory())
				.registerModule(new SimpleModule().addSerializer(NullObject.class, new JsonSerializer<NullObject>() {
					@Override
					public void serialize(NullObject nullObject, JsonGenerator jsonGenerator,
							SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
						jsonGenerator.writeNull();
					}
				}));
	}

	@Override
	protected String serializationType() {
		return "SMILE";
	}

	@Test
	@Override
	public void testWriteReadRemoteInovcation() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		byte[] data = writeRemoteInvocation(ri);

		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(JSONRPC).asText(), is(VERSION));
		assertThat(jsonNode.get(METHOD).asText(), is("echo"));
		assertThat(jsonNode.get(PARAMS).get(0).asText(), is("test"));
		assertThat(jsonNode.get(ID), is(notNullValue()));

		ri = readRemoteInvocation(TestService.class, data);
		assertThat(ri.getMethodName(), is("echo"));
		assertThat(ri.getArguments(), is(new Object[] { "test" }));
		assertThat(ri.getParameterTypes(), is(new Class<?>[] { String.class }));
	}

	@Test
	@Override
	public void testWriteReadRemoteInvocationResult() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		RemoteInvocationResult rir = new RemoteInvocationResult("result");
		byte[] data = writeRemoteInvocationResult(ri, rir);

		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(ID).asText(), is(notNullValue()));
		assertThat(jsonNode.get(RESULT).asText(), is("result"));

		rir = readRemoteInvocationResult(mi, data);
		assertThat(rir.getValue(), is("result"));
	}

	@Test
	@Override
	public void testWriteReadRemoteInvocationResultWithException()
			throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		RemoteInvocationResult rir = new RemoteInvocationResult(
				new InvocationTargetException(new RuntimeException("error")));
		byte[] data = writeRemoteInvocationResult(ri, rir);

		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(ID).asText(), is(notNullValue()));
		assertThat(jsonNode.get(ERROR).get(CODE).asInt(), is(CODE_INTERNAL_ERROR));
		assertThat(jsonNode.get(ERROR).get(MESSAGE).asText(), is("error"));
		assertThat(jsonNode.get(ERROR).get(DATA).asText(), is(RuntimeException.class.getName()));

		rir = readRemoteInvocationResult(mi, data);
		assertThat(rir.getValue(), is(nullValue()));
		assertThat(rir.hasInvocationTargetException(), is(true));
		assertThat(((InvocationTargetException) rir.getException()).getTargetException().getMessage(), is("error"));
		assertThat(((InvocationTargetException) rir.getException()).getTargetException() instanceof RuntimeException,
				is(true));
	}

	@Test
	@Override
	public void testWriteReadRemoteInvocationResultWithNull()
			throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("echo", String.class);
		MethodInvocation mi = createMethodInvocation(method, "test");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		RemoteInvocationResult rir = new RemoteInvocationResult();

		byte[] data = writeRemoteInvocationResult(ri, rir);
		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(ID).asText(), is(notNullValue()));
		assertThat(jsonNode.get(RESULT).isNull(), is(true));

		rir = readRemoteInvocationResult(mi, data);
		assertThat(rir.getValue(), is(nullValue()));
	}

	@Test
	public void testOptional() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("loadOptionalUserByUsername", String.class);
		MethodInvocation mi = createMethodInvocation(method, "username");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		User user = new User();
		user.setUsername("username");
		user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
		RemoteInvocationResult rir = new RemoteInvocationResult(user);

		byte[] data = writeRemoteInvocationResult(ri, rir);
		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(ID).asText(), is(notNullValue()));
		assertThat(jsonNode.get(RESULT).get("username").asText(), is("username"));

		rir = readRemoteInvocationResult(mi, data);
		assertThat(((User) rir.getValue()).getUsername(), is(user.getUsername()));
	}

	@Test
	public void testCallable() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("loadCallableUserByUsername", String.class);
		MethodInvocation mi = createMethodInvocation(method, "username");
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		User user = new User();
		user.setUsername("username");
		user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
		RemoteInvocationResult rir = new RemoteInvocationResult(user);

		byte[] data = writeRemoteInvocationResult(ri, rir);
		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(ID).asText(), is(notNullValue()));
		assertThat(jsonNode.get(RESULT).get("username").asText(), is("username"));

		rir = readRemoteInvocationResult(mi, data);
		assertThat(((User) rir.getValue()).getUsername(), is(user.getUsername()));
	}

	@Test
	public void testFuture() throws NoSuchMethodException, SecurityException, IOException {
		Method method = TestService.class.getDeclaredMethod("loadFutureUserByUsername", String.class, FutureType.class);
		MethodInvocation mi = createMethodInvocation(method, "username", FutureType.RUNNABLE);
		RemoteInvocation ri = serializer.createRemoteInvocation(mi);
		User user = new User();
		user.setUsername("username");
		user.setAuthorities(AuthorityUtils.createAuthorityList("test"));
		RemoteInvocationResult rir = new RemoteInvocationResult(user);

		byte[] data = writeRemoteInvocationResult(ri, rir);
		JsonNode jsonNode = objectMapper.readTree(data);
		assertThat(jsonNode.get(ID).asText(), is(notNullValue()));
		assertThat(jsonNode.get(RESULT).get("username").asText(), is("username"));

		rir = readRemoteInvocationResult(mi, data);
		assertThat(((User) rir.getValue()).getUsername(), is(user.getUsername()));
	}
}