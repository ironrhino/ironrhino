package org.ironrhino.core.remoting.serializer;

import org.ironrhino.core.remoting.RemotingContext;

import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class CborHttpInvokerSerializer extends AbstractJsonRpcHttpInvokerSerializer {

	public static CborHttpInvokerSerializer INSTANCE = new CborHttpInvokerSerializer();

	private CborHttpInvokerSerializer() {
		super(new CBORFactory());
	}

	@Override
	public String getContentType() {
		return RemotingContext.CONTENT_TYPE_CBOR_RPC;
	}

}
