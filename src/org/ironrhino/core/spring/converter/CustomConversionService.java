package org.ironrhino.core.spring.converter;

import org.springframework.core.convert.support.DefaultConversionService;

public class CustomConversionService extends DefaultConversionService {

	public CustomConversionService() {
		super();
		addConverter(new DateConverter());
		addConverterFactory(new IntegerToEnumConverterFactory());
		addConverter(Enum.class, Integer.class, new EnumToIntegerConverter((this)));
		addConverter(new EnumToEnumConverter());
		addConverter(new SerializableToSerializableConverter());
	}

}
