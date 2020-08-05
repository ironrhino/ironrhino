package org.ironrhino.core.spring.converter;

import java.time.MonthDay;

import org.springframework.core.convert.converter.Converter;

public class MonthDayConverter implements Converter<String, MonthDay> {

	@Override
	public MonthDay convert(String source) {
		return MonthDay.parse(source);
	}

}