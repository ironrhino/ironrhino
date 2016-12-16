package org.ironrhino.core.spring.converter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.util.JsonUtils;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class StringToMapConverter implements GenericConverter {

	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new GenericConverter.ConvertiblePair(String.class, Map.class));
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		String string = (String) source;
		try {
			TypeFactory tf = JsonUtils.getObjectMapper().getTypeFactory();
			ResolvableType rt = targetType.getResolvableType();
			JavaType jt = tf.constructParametricType(rt.getRawClass(), tf.constructType(rt.getGeneric(0).getRawClass()),
					tf.constructType(rt.getGeneric(1).getRawClass()));
			return JsonUtils.fromJson(string, jt);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
