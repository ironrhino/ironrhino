package org.ironrhino.core.spring.converter;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.ironrhino.core.util.DateUtils;
import org.springframework.core.convert.converter.Converter;

public class LocalDateTimeConverter implements Converter<String, LocalDateTime> {

	@Override
	public LocalDateTime convert(String source) {
		return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

}