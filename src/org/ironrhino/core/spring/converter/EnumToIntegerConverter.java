package org.ironrhino.core.spring.converter;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

public final class EnumToIntegerConverter implements Converter<Enum<?>, Integer>, ConditionalConverter {

	private final ConversionService conversionService;

	public EnumToIntegerConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClass(sourceType.getType())) {
			if (conversionService.canConvert(TypeDescriptor.valueOf(interfaceType), targetType)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Integer convert(Enum<?> source) {
		return source.ordinal();
	}

}
