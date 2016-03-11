package org.ironrhino.core.spring.converter;

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

public  class StringToEnumConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, Enum.class));
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null)
				return null;
			return Enum.valueOf((Class<Enum>) targetType.getType(), (String) source);
		}

	}