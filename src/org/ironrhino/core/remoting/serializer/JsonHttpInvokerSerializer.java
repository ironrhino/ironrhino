package org.ironrhino.core.remoting.serializer;

import org.ironrhino.core.remoting.RemotingContext;

public class JsonHttpInvokerSerializer extends JacksonHttpInvokerSerializer {

	public static JsonHttpInvokerSerializer INSTANCE = new JsonHttpInvokerSerializer();

	private JsonHttpInvokerSerializer() {
		super(null);
	}

	@Override
	public String getContentType() {
		return RemotingContext.CONTENT_TYPE_JSON_RPC;
	}

}
