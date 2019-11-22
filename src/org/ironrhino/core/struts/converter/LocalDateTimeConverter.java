package org.ironrhino.core.struts.converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.core.util.DateUtils;

@SuppressWarnings("rawtypes")
public class LocalDateTimeConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().isEmpty())
			return null;
		String source = values[0].trim();
		try {
			return LocalDateTime.parse(source);
		} catch (DateTimeParseException e) {
			return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof LocalDateTime)
			return DateUtils.formatDatetime(Date.from(((LocalDateTime) o).atZone(ZoneId.systemDefault()).toInstant()));
		return "";
	}

}
