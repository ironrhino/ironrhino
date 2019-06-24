package org.ironrhino.core.remoting.serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;

public class SmileHttpInvokerSerializerTest extends AbstractJsonHttpInvokerSerializerTestBase {

	@Override
	protected JsonFactory jsonFactory() {
		return new SmileFactory();
	}

	@Override
	protected String serializationType() {
		return "SMILE";
	}

}