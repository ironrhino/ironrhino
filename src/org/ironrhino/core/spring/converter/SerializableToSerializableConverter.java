package org.ironrhino.core.spring.converter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.ironrhino.core.util.BeanUtils;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

public class SerializableToSerializableConverter implements ConditionalGenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Serializable.class, Serializable.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.getType().equals(targetType.getType()))
			return false;
		try {
			sourceType.getType().getConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
		try {
			targetType.getType().getConstructor();
			return true;
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null)
			return null;
		try {
			Object target = targetType.getType().newInstance();
			BeanUtils.copyProperties(source, target);
			return target;
		} catch (Exception e) {
			return null;
		}
	}

}
