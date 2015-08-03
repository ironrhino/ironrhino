package org.ironrhino.core.spring.converter;

import java.util.Collections;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

public class EnumToEnumConverter implements ConditionalGenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Enum.class, Enum.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return sourceType.getObjectType() != targetType.getObjectType();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if(source == null)
			return null;
		Enum<?> s = (Enum<?>) source;
		try {
			return Enum.valueOf((Class<Enum>) targetType.getObjectType(), s.name());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

}