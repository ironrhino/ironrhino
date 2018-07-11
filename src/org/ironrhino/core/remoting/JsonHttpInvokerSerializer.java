package org.ironrhino.core.remoting;

public class JsonHttpInvokerSerializer extends JacksonHttpInvokerSerializer {

	public static JsonHttpInvokerSerializer INSTANCE = new JsonHttpInvokerSerializer();

	private JsonHttpInvokerSerializer() {
		super(null);
	}

	@Override
	public String getContentType() {
		return RemotingContext.CONTENT_TYPE_JSON_SERIALIZED_OBJECT;
	}

}
