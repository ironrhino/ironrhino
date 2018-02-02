package org.ironrhino.core.spring.converter;

import java.time.LocalTime;

import org.springframework.core.convert.converter.Converter;

public class LocalTimeConverter implements Converter<String, LocalTime> {

	@Override
	public LocalTime convert(String source) {
		return LocalTime.parse(source);
	}

}