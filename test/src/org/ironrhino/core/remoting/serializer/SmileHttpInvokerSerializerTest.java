package org.ironrhino.core.remoting.serializer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
		assertEquals(VERSION, jsonNode.get(JSONRPC).asText());
		assertEquals("echo", jsonNode.get(METHOD).asText());
		assertEquals("test", jsonNode.get(PARAMS).get(0).asText());
		assertNotNull(jsonNode.get(ID));

		ri = readRemoteInvocation(TestService.class, data);
		assertEquals("echo", ri.getMethodName());
		assertArrayEquals(new Object[] { "test" }, ri.getArguments());
		assertArrayEquals(new Class<?>[] { String.class }, ri.getParameterTypes());
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
		assertNotNull(jsonNode.get(ID).asText());
		assertEquals("result", jsonNode.get(RESULT).asText());

		rir = readRemoteInvocationResult(mi, data);
		assertEquals("result", rir.getValue());
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
		assertNotNull(jsonNode.get(ID).asText());
		assertEquals(CODE_INTERNAL_ERROR, jsonNode.get(ERROR).get(CODE).asInt());
		assertEquals("error", jsonNode.get(ERROR).get(MESSAGE).asText());
		assertEquals(RuntimeException.class.getName(), jsonNode.get(ERROR).get(DATA).asText());

		rir = readRemoteInvocationResult(mi, data);
		assertNull(rir.getValue());
		assertTrue(rir.hasInvocationTargetException());
		assertEquals("error", ((InvocationTargetException) rir.getException()).getTargetException().getMessage());
		assertTrue(((InvocationTargetException) rir.getException()).getTargetException() instanceof RuntimeException);
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
		assertNotNull(jsonNode.get(ID).asText());
		assertTrue(jsonNode.get(RESULT).isNull());

		rir = readRemoteInvocationResult(mi, data);
		assertNull(rir.getValue());
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
		assertNotNull(jsonNode.get(ID).asText());
		assertEquals("username", jsonNode.get(RESULT).get("username").asText());

		rir = readRemoteInvocationResult(mi, data);
		assertEquals(user.getUsername(), ((User) rir.getValue()).getUsername());
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
		assertNotNull(jsonNode.get(ID).asText());
		assertEquals("username", jsonNode.get(RESULT).get("username").asText());

		rir = readRemoteInvocationResult(mi, data);
		assertEquals(user.getUsername(), ((User) rir.getValue()).getUsername());
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
		assertNotNull(jsonNode.get(ID).asText());
		assertEquals("username", jsonNode.get(RESULT).get("username").asText());

		rir = readRemoteInvocationResult(mi, data);
		assertEquals(user.getUsername(), ((User) rir.getValue()).getUsername());
	}
}