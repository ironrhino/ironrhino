package org.ironrhino.core.struts.converter;

import java.time.Duration;
import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;

@SuppressWarnings("rawtypes")
public class DurationConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().equals(""))
			return null;
		return Duration.parse(values[0].trim());
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof Duration)
			return o.toString();
		return "";
	}

}
