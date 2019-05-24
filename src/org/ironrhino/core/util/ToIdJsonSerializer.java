package org.ironrhino.core.util;

import java.io.IOException;

import org.springframework.beans.BeanWrapperImpl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class ToIdJsonSerializer extends StdSerializer<Object> {

	private static final long serialVersionUID = -7683493644667066248L;

	public ToIdJsonSerializer() {
		this(null);
	}

	public ToIdJsonSerializer(Class<Object> t) {
		super(t);
	}

	@Override
	public void serialize(Object obj, JsonGenerator generator, SerializerProvider sp) throws IOException {
		Object id = new BeanWrapperImpl(obj).getPropertyValue("id");
		if (id instanceof Number)
			generator.writeNumber(((Number) id).longValue());
		else if (id != null)
			generator.writeString(id.toString());
	}

}
