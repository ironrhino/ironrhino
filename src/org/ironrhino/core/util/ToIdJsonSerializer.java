package org.ironrhino.core.util;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
		if (obj == null)
			return;
		if (obj instanceof Collection) {
			List<Object> ids = ((Collection<?>) obj).stream().map(o -> new BeanWrapperImpl(o).getPropertyValue("id"))
					.collect(Collectors.toList());
			generator.writeObject(ids);
		} else if (obj instanceof Object[]) {
			List<Object> ids = Stream.of((Object[]) obj).map(o -> new BeanWrapperImpl(o).getPropertyValue("id"))
					.collect(Collectors.toList());
			generator.writeObject(ids);
		} else {
			Object id = new BeanWrapperImpl(obj).getPropertyValue("id");
			generator.writeObject(id);
		}
	}

	public static void main(String[] args) {
		System.out.println();
	}

}
