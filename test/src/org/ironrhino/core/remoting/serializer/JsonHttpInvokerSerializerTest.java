package org.ironrhino.core.remoting.serializer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.ironrhino.core.remoting.serializer.AbstractJsonRpcHttpInvokerSerializer.JsonRpcException;
import org.ironrhino.sample.remoting.TestService;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;

public class JsonHttpInvokerSerializerTest extends AbstractJsonHttpInvokerSerializerTest {

	@Override
	protected JsonFactory jsonFactory() {
		return null;
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
			assertThat(e, is(notNullValue()));
			assertThat(e instanceof JsonRpcException, is(true));
			assertThat(((JsonRpcException) e).getCode(), is(CODE_PARSE_ERROR));
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
			assertThat(e, is(notNullValue()));
			assertThat(e instanceof JsonRpcException, is(true));
			assertThat(((JsonRpcException) e).getCode(), is(CODE_INVALID_REQUEST));
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
			assertThat(e, is(notNullValue()));
			assertThat(e instanceof JsonRpcException, is(true));
			assertThat(((JsonRpcException) e).getCode(), is(CODE_INVALID_PARAMS));
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
			assertThat(e, is(notNullValue()));
			assertThat(e instanceof JsonRpcException, is(true));
			assertThat(((JsonRpcException) e).getCode(), is(CODE_METHOD_NOT_FOUND));
		}
	}
}