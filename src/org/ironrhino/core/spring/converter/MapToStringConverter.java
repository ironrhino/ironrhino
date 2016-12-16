package org.ironrhino.core.spring.converter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.ironrhino.core.util.JsonUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

public class MapToStringConverter implements GenericConverter {

	public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new GenericConverter.ConvertiblePair(Map.class, String.class));
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return null;
		}
		return JsonUtils.toJson(source);
	}

}
