package org.ironrhino.core.spring.converter;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.ironrhino.core.util.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class ZonedDateTimeConverter implements Converter<String, ZonedDateTime> {

	@Override
	public ZonedDateTime convert(String source) {
		try {
			return ZonedDateTime.parse(source);
		} catch (DateTimeParseException e) {
			return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault());
		}
	}

}