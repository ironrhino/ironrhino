
package org.ironrhino.core.jdbc;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class NestedPathMapSqlParameterSource extends MapSqlParameterSource {

	public NestedPathMapSqlParameterSource() {

	}

	public NestedPathMapSqlParameterSource(Map<String, Object> map) {
		super(map);
	}

	public boolean hasValue(String paramName) {
		boolean b = super.hasValue(paramName);
		if (!b && paramName.indexOf('.') > 0) {
			String[] arr = paramName.split("\\.", 2);
			if (super.hasValue(arr[0])) {
				Object object = getValue(arr[0]);
				EntityBeanPropertySqlParameterSource source = new EntityBeanPropertySqlParameterSource(object);
				return source.hasValue(arr[1]);
			}
		}
		return b;
	}

	public Object getValue(String paramName) throws IllegalArgumentException {
		Object value;
		if (paramName.indexOf('.') > 0) {
			String[] arr = paramName.split("\\.", 2);
			Object object = getValue(arr[0]);
			if (object == null)
				return null;
			EntityBeanPropertySqlParameterSource source = new EntityBeanPropertySqlParameterSource(object);
			value = source.getValue(arr[1]);
		} else {
			value = super.getValue(paramName);
			if (value instanceof Enum) {
				value = ((Enum<?>) value).name();
			}
		}
		return value;
	}

}
