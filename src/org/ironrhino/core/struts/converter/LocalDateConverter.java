package org.ironrhino.core.struts.converter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.core.util.DateUtils;

@SuppressWarnings("rawtypes")
public class LocalDateConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().equals(""))
			return null;
		String source = values[0].trim();
		try {
			return LocalDate.parse(source);
		} catch (DateTimeParseException e) {
			return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		}
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof LocalDate)
			return DateUtils.formatDate10(Date.from(((LocalDate) o).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return "";
	}

}
