package org.ironrhino.core.spring.converter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.ironrhino.core.util.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class LocalDateConverter implements Converter<String, LocalDate> {

	@Override
	public LocalDate convert(String source) {
		try {
			return LocalDate.parse(source);
		} catch (DateTimeParseException e) {
			return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
	}

}