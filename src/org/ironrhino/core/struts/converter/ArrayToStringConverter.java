package org.ironrhino.core.struts.converter;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.util.StrutsTypeConverter;

@SuppressWarnings("rawtypes")
public class ArrayToStringConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().equals(""))
			return null;
		return StringUtils.join(values, ",");
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o == null)
			return null;
		return (o instanceof String[]) ? StringUtils.join((String[]) o, ",") : o.toString();
	}

}
