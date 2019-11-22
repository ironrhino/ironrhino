package org.ironrhino.core.struts.converter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;

@SuppressWarnings("rawtypes")
public class LocalTimeConverter extends StrutsTypeConverter {

	private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().isEmpty())
			return null;
		return LocalTime.parse(values[0].trim());
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof LocalTime)
			return ((LocalTime) o).format(dateTimeFormatter);
		return "";
	}

}
