package org.ironrhino.core.spring.converter;

import java.time.Duration;

import org.springframework.core.convert.converter.Converter;

public class DurationConverter implements Converter<String, Duration> {

	@Override
	public Duration convert(String source) {
		return Duration.parse(source);
	}

}