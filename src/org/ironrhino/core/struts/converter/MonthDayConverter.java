package org.ironrhino.core.struts.converter;

import java.time.MonthDay;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;

@SuppressWarnings("rawtypes")
public class MonthDayConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().isEmpty())
			return null;
		return MonthDay.parse(values[0].trim());
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof MonthDay)
			return o.toString();
		return "";
	}

}
