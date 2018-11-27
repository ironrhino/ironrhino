package org.ironrhino.core.remoting.serializer;

import org.ironrhino.core.remoting.RemotingContext;

import com.fasterxml.jackson.dataformat.smile.SmileFactory;

public class SmileHttpInvokerSerializer extends JacksonHttpInvokerSerializer {

	public static SmileHttpInvokerSerializer INSTANCE = new SmileHttpInvokerSerializer();

	private SmileHttpInvokerSerializer() {
		super(new SmileFactory());
	}

	@Override
	public String getContentType() {
		return RemotingContext.CONTENT_TYPE_SMILE_RPC;
	}

}
