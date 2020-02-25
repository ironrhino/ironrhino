package org.ironrhino.core.spring.converter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.ironrhino.core.util.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class OffsetDateTimeConverter implements Converter<String, OffsetDateTime> {

	@Override
	public OffsetDateTime convert(String source) {
		try {
			return OffsetDateTime.parse(source);
		} catch (DateTimeParseException e) {
			return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
		}
	}

}