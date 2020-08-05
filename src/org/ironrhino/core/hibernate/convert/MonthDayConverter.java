package org.ironrhino.core.hibernate.convert;

import java.time.MonthDay;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class MonthDayConverter implements AttributeConverter<MonthDay, String> {

	@Override
	public String convertToDatabaseColumn(MonthDay obj) {
		if (obj == null)
			return null;
		return obj.toString();
	}

	@Override
	public MonthDay convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		return MonthDay.parse(string);
	}

}