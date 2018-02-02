package org.ironrhino.core.spring.converter;

import java.time.LocalDate;
import java.time.ZoneId;

import org.ironrhino.core.util.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class LocalDateConverter implements Converter<String, LocalDate> {

	@Override
	public LocalDate convert(String source) {
		return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

}