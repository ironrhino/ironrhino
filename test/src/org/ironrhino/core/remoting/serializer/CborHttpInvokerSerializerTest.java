package org.ironrhino.core.remoting.serializer;

import java.io.IOException;

import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.util.JsonSerializationUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

public class CborHttpInvokerSerializerTest extends SmileHttpInvokerSerializerTest {

	public CborHttpInvokerSerializerTest() {
		this.objectMapper = JsonSerializationUtils.createNewObjectMapper(new CBORFactory())
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
		return "CBOR";
	}

}