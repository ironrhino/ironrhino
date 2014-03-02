package org.ironrhino.common.util;

import java.util.Map;

import org.apache.struts2.util.StrutsTypeConverter;
import org.ironrhino.common.model.Coordinate;

@SuppressWarnings("rawtypes")
public class CoordinateConverter extends StrutsTypeConverter {

	@Override
	public Object convertFromString(Map context, String[] values, Class toClass) {
		if (values[0] == null || values[0].trim().equals(""))
			return null;
		return new Coordinate(values[0].trim());
	}

	@Override
	public String convertToString(Map arg0, Object o) {
		if (o instanceof Coordinate)
			return ((Coordinate) o).getLatLngAsString();
		return "";
	}

}
