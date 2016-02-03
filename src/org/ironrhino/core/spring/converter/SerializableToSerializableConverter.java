package org.ironrhino.core.spring.converter;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.ironrhino.core.util.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

public class SerializableToSerializableConverter implements ConditionalGenericConverter {

	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Serializable.class, Serializable.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Class<?> sourceClass = sourceType.getType();
		Class<?> targetClass = targetType.getType();
		if (sourceClass.equals(targetClass))
			return false;
		try {
			sourceClass.getConstructor();
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
		if (targetClass.equals(String.class) || targetClass.equals(Long.class) || targetClass.equals(Long.TYPE)
				|| targetClass.equals(Integer.class) || targetClass.equals(Integer.TYPE)) {
			BeanWrapperImpl bw = new BeanWrapperImpl(sourceClass);
			PropertyDescriptor pd = bw.getPropertyDescriptor("id");
			if (pd != null && pd.getPropertyType().equals(targetClass))
				return true;
		}
		try {
			targetClass.getConstructor();
			return true;
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	@Override
	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null)
			return null;
		Class<?> targetClass = targetType.getType();
		if (targetClass.equals(String.class) || targetClass.equals(Long.class) || targetClass.equals(Long.TYPE)
				|| targetClass.equals(Integer.class) || targetClass.equals(Integer.TYPE)) {
			BeanWrapperImpl bw = new BeanWrapperImpl(source);
			return bw.getPropertyValue("id");
		}
		try {
			Object target = targetClass.newInstance();
			BeanUtils.copyProperties(source, target);
			return target;
		} catch (Exception e) {
			return null;
		}
	}

}
