package org.ironrhino.core.util;

import java.io.IOException;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class FromIdJsonDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {

	private static final long serialVersionUID = 2685701643083493128L;

	public FromIdJsonDeserializer() {
		this(null);
	}

	public FromIdJsonDeserializer(Class<Object> t) {
		super(t);
	}

	@Override
	public Object deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException, JsonProcessingException {
		try {
			Object obj = BeanUtils.instantiateClass(this._valueClass);
			BeanWrapperImpl bw = new BeanWrapperImpl(obj);
			bw.setPropertyValue("id", parser.getText());
			return obj;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public JsonDeserializer<Object> createContextual(DeserializationContext ctx, BeanProperty beanProperty)
			throws JsonMappingException {
		@SuppressWarnings("unchecked")
		Class<Object> clazz = (Class<Object>) beanProperty.getType().getRawClass();
		return new FromIdJsonDeserializer(clazz);
	}

}
