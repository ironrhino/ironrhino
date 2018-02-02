package org.ironrhino.core.struts.converter;

import java.time.LocalDate;
import java.time.ZoneId;
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
		return DateUtils.parse(values[0].trim()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof LocalDate)
			return DateUtils.formatDate10(Date.from(((LocalDate) o).atStartOfDay(ZoneId.systemDefault()).toInstant()));
		return "";
	}

}
