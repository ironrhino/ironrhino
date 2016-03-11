package org.ironrhino.core.jdbc;

import org.ironrhino.core.spring.converter.IntegerToEnumConverter;
import org.ironrhino.core.spring.converter.StringToEnumConverter;
import org.springframework.beans.BeanWrapper;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

public class EnumSupportedRowMapper<T> extends BeanPropertyRowMapper<T> {

	static DefaultConversionService conversionService = new DefaultConversionService();

	static {
		conversionService.addConverter(new StringToEnumConverter());
		conversionService.addConverter(new IntegerToEnumConverter());
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
