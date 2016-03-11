package org.ironrhino.core.jdbc;

import org.ironrhino.core.spring.converter.EnumToIntegerConverter;
import org.ironrhino.core.spring.converter.IntegerToEnumConverterFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

public class EnumSupportedRowMapper<T> extends BeanPropertyRowMapper<T> {

	static DefaultConversionService conversionService = new DefaultConversionService();

	static {
		conversionService.addConverterFactory(new IntegerToEnumConverterFactory());
		conversionService.addConverter(Enum.class, Integer.class,
				new EnumToIntegerConverter((conversionService)));
	}

	public EnumSupportedRowMapper() {
	}

	public EnumSupportedRowMapper(Class<T> mappedClass) {
		initialize(mappedClass);
	}

	@Override
	protected void initBeanWrapper(BeanWrapper bw) {
		bw.setConversionService(conversionService);
	}

}
