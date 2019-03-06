package org.ironrhino.core.remoting.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.remoting.serializer.AbstractJsonRpcHttpInvokerSerializer.JsonRpcException;
import org.ironrhino.core.util.JsonSerializationUtils;
import org.ironrhino.sample.remoting.TestService;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JsonHttpInvokerSerializerTest extends SmileHttpInvokerSerializerTest {

	public JsonHttpInvokerSerializerTest() {
		objectMapper = JsonSerializationUtils.createNewObjectMapper(null)
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
		return "JSON";
	}

	@Test
	public void testParseError() {
		String[] jsons = { "", "{\"jsonrpc\":2.0\"}" };
		for (String json : jsons) {
			Exception e = null;
			try {
				readRemoteInvocation(null, json.getBytes());
			} catch (Exception error) {
				e = error;
			}
			assertNotNull(e);
			assertTrue(e instanceof JsonRpcException);
			assertEquals(CODE_PARSE_ERROR, ((JsonRpcException) e).getCode());
		}
	}

	@Test
	public void testInvalidRequest() {
		String[] jsons = { "{\"jsonrpc\":\"" + VERSION
				+ "\",\"method\":\"echo\",\"params\":[\"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\", \"test\":\"test\"}",
				"{\"method\":\"echo\",\"params\":[\"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}",
				"{\"jsonrpc\":2.0,\"method\":\"echo\",\"params\":[\"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}",
				"{\"jsonrpc\":\"" + VERSION + "\",\"params\":[\"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}",
				"{\"jsonrpc\":\"" + VERSION
						+ "\",\"method\":123,\"params\":[\"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}",
				"{\"jsonrpc\":\"" + VERSION
						+ "\",\"method\":\"echo\",\"params\":\"\",\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}" };
		for (String json : jsons) {
			Exception e = null;
			try {
				readRemoteInvocation(null, json.getBytes());
			} catch (Exception error) {
				e = error;
			}
			assertNotNull(e);
			assertTrue(e instanceof JsonRpcException);
			assertEquals(CODE_INVALID_REQUEST, ((JsonRpcException) e).getCode());
		}
	}

	@Test
	public void testInvalidParam() {
		String[] jsons = { "{\"jsonrpc\":\"" + VERSION
				+ "\",\"method\":\"echo\",\"params\":[\"test\", \"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}" };
		for (String json : jsons) {
			Exception e = null;
			try {
				readRemoteInvocation(TestService.class, json.getBytes());
			} catch (Exception error) {
				e = error;
			}
			assertNotNull(e);
			assertTrue(e instanceof JsonRpcException);
			assertEquals(CODE_INVALID_PARAMS, ((JsonRpcException) e).getCode());
		}
	}

	@Test
	public void testMethodNotFound() {
		String[] jsons = { "{\"jsonrpc\":\"" + VERSION
				+ "\",\"method\":\"echo1\",\"params\":[\"test\"],\"id\":\"1ZO93Uw0YAgHPAXfbsSuwk\"}" };
		for (String json : jsons) {
			Exception e = null;
			try {
				readRemoteInvocation(TestService.class, json.getBytes());
			} catch (Exception error) {
				e = error;
			}
			assertNotNull(e);
			assertTrue(e instanceof JsonRpcException);
			assertEquals(CODE_METHOD_NOT_FOUND, ((JsonRpcException) e).getCode());
		}
	}
}