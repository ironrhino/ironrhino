package org.ironrhino.core.hibernate.convert;

import java.time.YearMonth;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

	@Override
	public String convertToDatabaseColumn(YearMonth obj) {
		if (obj == null)
			return null;
		return obj.toString();
	}

	@Override
	public YearMonth convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		return YearMonth.parse(string);
	}

}