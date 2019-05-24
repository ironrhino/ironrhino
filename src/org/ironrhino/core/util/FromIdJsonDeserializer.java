package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class FromIdJsonDeserializer extends StdDeserializer<Object> implements ContextualDeserializer {

	private static final long serialVersionUID = 2685701643083493128L;

	private JavaType type;

	public FromIdJsonDeserializer() {
		this((Class<Object>) null);
	}

	public FromIdJsonDeserializer(Class<Object> t) {
		super(t);
	}

	public FromIdJsonDeserializer(JavaType type) {
		super((Class<Object>) null);
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object deserialize(JsonParser parser, DeserializationContext ctx)
			throws IOException, JsonProcessingException {
		if (type == null)
			return null;
		try {
			if (type.isCollectionLikeType() || type.isArrayType()) {
				Collection<Object> coll = null;
				Class<?> componentType = type.getContentType().getRawClass();
				if (type.isArrayType()) {
					coll = new ArrayList<>();
				} else {
					Class<?> clazz = type.getRawClass();
					if (type.isConcrete()) {
						coll = (Collection<Object>) BeanUtils.instantiateClass(clazz);
					} else if (clazz.isAssignableFrom(ArrayList.class)) {
						coll = new ArrayList<>();
					} else {
						coll = new LinkedHashSet<>();
					}
				}
				if (parser.currentToken() != JsonToken.START_ARRAY)
					throw new RuntimeException("Not array node");
				while (parser.nextToken() != JsonToken.END_ARRAY) {
					switch (parser.currentToken()) {
					case START_OBJECT:
						coll.add(parser.readValueAs(componentType));
						break;
					default:
						Object obj = BeanUtils.instantiateClass(componentType);
						BeanWrapperImpl bw = new BeanWrapperImpl(obj);
						bw.setPropertyValue("id", parser.getText());
						coll.add(obj);
					}
				}
				if (type.isArrayType()) {
					List<Object> list = (List<Object>) coll;
					Object array = Array.newInstance(componentType, list.size());
					for (int i = 0; i < list.size(); i++)
						Array.set(array, i, list.get(i));
					return array;
				} else {
					return coll;
				}
			} else if (type.isConcrete()) {
				Object obj;
				if (!parser.currentToken().isScalarValue()) {
					obj = parser.readValueAs(type.getRawClass());
				} else {
					obj = BeanUtils.instantiateClass(type.getRawClass());
					BeanWrapperImpl bw = new BeanWrapperImpl(obj);
					bw.setPropertyValue("id", parser.getText());
				}
				return obj;
			} else {
				throw new RuntimeException("cannot deserialize " + type);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonDeserializer<Object> createContextual(DeserializationContext ctx, BeanProperty beanProperty)
			throws JsonMappingException {
		return new FromIdJsonDeserializer(beanProperty.getType());
	}

}
