package org.ironrhino.core.util;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
			if (type.isCollectionLikeType()) {
				TreeNode node = parser.readValueAsTree();
				if (!(node instanceof ArrayNode))
					throw new RuntimeException("Not array node");
				ArrayNode array = (ArrayNode) node;
				Collection<Object> coll = null;
				Class<?> clazz = type.getRawClass();
				Class<?> componentType = type.getContentType().getRawClass();
				if (type.isConcrete()) {
					coll = (Collection<Object>) BeanUtils.instantiateClass(clazz);
				} else if (clazz.isAssignableFrom(ArrayList.class)) {
					coll = new ArrayList<>();
				} else {
					coll = new LinkedHashSet<>();
				}
				for (int i = 0; i < array.size(); i++) {
					Object obj = BeanUtils.instantiateClass(componentType);
					BeanWrapperImpl bw = new BeanWrapperImpl(obj);
					bw.setPropertyValue("id", array.get(i).asText());
					coll.add(obj);
				}
				return coll;
			} else if (type.isArrayType()) {
				TreeNode node = parser.readValueAsTree();
				if (!(node instanceof ArrayNode))
					throw new RuntimeException("Not array node");
				ArrayNode array = (ArrayNode) node;
				Class<?> componentType = type.getContentType().getRawClass();
				Object result = Array.newInstance(componentType, array.size());
				for (int i = 0; i < array.size(); i++) {
					Object obj = BeanUtils.instantiateClass(componentType);
					BeanWrapperImpl bw = new BeanWrapperImpl(obj);
					bw.setPropertyValue("id", array.get(i).asText());
					Array.set(result, i, obj);
				}
				return result;
			} else if (type.isConcrete()) {
				Object obj = BeanUtils.instantiateClass(type.getRawClass());
				BeanWrapperImpl bw = new BeanWrapperImpl(obj);
				bw.setPropertyValue("id", parser.getText());
				return obj;
			} else {
				throw new RuntimeException("cannot deserialize " + type);
			}
		} catch (

		Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public JsonDeserializer<Object> createContextual(DeserializationContext ctx, BeanProperty beanProperty)
			throws JsonMappingException {
		return new FromIdJsonDeserializer(beanProperty.getType());
	}

}
