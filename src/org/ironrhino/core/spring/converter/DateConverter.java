package org.ironrhino.core.spring.converter;

import java.util.Date;

import org.ironrhino.core.util.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class DateConverter implements Converter<String, Date> {

	@Override
	public Date convert(String source) {
		return DateUtils.parse(source);
	}

}