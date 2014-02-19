package org.ironrhino.core.struts.converter;

import java.util.Date;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.core.util.DateUtils;

@SuppressWarnings("rawtypes")
public class DateConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().equals(""))
			return null;
		return DateUtils.parse(values[0].trim());
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof Date)
			return DateUtils.formatDate10((Date) o);
		return "";
	}

}
