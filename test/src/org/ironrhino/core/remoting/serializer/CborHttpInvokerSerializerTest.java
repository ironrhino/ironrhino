package org.ironrhino.core.remoting.serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class CborHttpInvokerSerializerTest extends AbstractJsonHttpInvokerSerializerTestBase {

	@Override
	protected JsonFactory jsonFactory() {
		return new CBORFactory();
	}

	@Override
	protected String serializationType() {
		return "CBOR";
	}

}