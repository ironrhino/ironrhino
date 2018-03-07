package org.ironrhino.core.spring.converter;

import java.time.YearMonth;

import org.springframework.core.convert.converter.Converter;

public class YearMonthConverter implements Converter<String, YearMonth> {

	@Override
	public YearMonth convert(String source) {
		return YearMonth.parse(source);
	}

}