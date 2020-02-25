package org.ironrhino.core.struts.converter;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.core.util.DateUtils;

@SuppressWarnings("rawtypes")
public class OffsetDateTimeConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().isEmpty())
			return null;
		String source = values[0].trim();
		try {
			return OffsetDateTime.parse(source);
		} catch (DateTimeParseException e) {
			return DateUtils.parse(source).toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
		}
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof OffsetDateTime)
			return DateUtils.formatDatetime(Date.from(((OffsetDateTime) o).toInstant()));
		return "";
	}

}
