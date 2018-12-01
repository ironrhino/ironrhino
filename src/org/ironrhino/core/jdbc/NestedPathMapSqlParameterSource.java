
package org.ironrhino.core.jdbc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public class NestedPathMapSqlParameterSource extends MapSqlParameterSource {

	public NestedPathMapSqlParameterSource() {

	}

	public NestedPathMapSqlParameterSource(Map<String, Object> map) {
		super(map);
	}

	@Override
	public boolean hasValue(String paramName) {
		if (super.hasValue(paramName))
			return true;
		int index1 = paramName.indexOf('[');
		int index2 = paramName.indexOf(']');
		int index3 = paramName.indexOf('.');
		if (index1 > 0 && index2 <= index1 || index3 > 0 && index3 == paramName.length() - 1)
			return false;
		if (index1 > 0 && (index3 < 0 || index3 == index2 + 1)) {
			String top = paramName.substring(0, index1);
			String sub = paramName.substring(index1 + 1, index2);
			Object topValue = super.hasValue(top) ? topValue = super.getValue(top) : null;
			if (topValue instanceof Object[]) {
				if (!StringUtils.isNumeric(sub))
					return false;
				int index = Integer.valueOf(sub);
				Object[] array = (Object[]) topValue;
				if (index < 0 || index >= array.length)
					return false;
				topValue = array[index];
			} else if (topValue instanceof Collection) {
				if (!StringUtils.isNumeric(sub))
					return false;
				int index = Integer.valueOf(sub);
				Collection<?> collection = (Collection<?>) topValue;
				if (index < 0 || index >= collection.size())
					return false;
				Iterator<?> it = collection.iterator();
				int i = 0;
				while (it.hasNext()) {
					topValue = it.next();
					if (i == index)
						break;
					i++;
				}
			} else if (topValue instanceof Map) {
				if (!(sub.startsWith("'") && sub.endsWith("'") || sub.startsWith("\"") && sub.endsWith("\"")))
					return false;
				sub = sub.substring(1, sub.length() - 1);
				Map<?, ?> map = ((Map<?, ?>) topValue);
				if (!map.containsKey(sub))
					return false;
				topValue = map.get(sub);
			} else {
				return false;
			}
			if (index2 == paramName.length() - 1)
				return true;
			if (index3 == index2 + 1) {
				if (topValue == null)
					return false;
				EntityBeanPropertySqlParameterSource source = new EntityBeanPropertySqlParameterSource(topValue);
				return source.hasValue(paramName.substring(index3 + 1));
			}
		}
		if (index3 > 0) {
			String top = paramName.substring(0, index3);
			if (super.hasValue(top)) {
				Object topValue = getValue(top);
				if (topValue == null)
					return false;
				EntityBeanPropertySqlParameterSource source = new EntityBeanPropertySqlParameterSource(topValue);
				return source.hasValue(paramName.substring(index3 + 1));
			}
		}
		return false;
	}

	@Override
	public Object getValue(String paramName) throws IllegalArgumentException {
		if (super.hasValue(paramName))
			return super.getValue(paramName);
		int index1 = paramName.indexOf('[');
		int index2 = paramName.indexOf(']');
		int index3 = paramName.indexOf('.');
		if (index1 > 0 && index2 <= index1 || index3 > 0 && index3 == paramName.length() - 1)
			throw new IllegalArgumentException("Invalid param name: " + paramName);
		if (index1 > 0 && (index3 < 0 || index3 == index2 + 1)) {
			String top = paramName.substring(0, index1);
			String sub = paramName.substring(index1 + 1, index2);
			Object topValue = super.hasValue(top) ? topValue = super.getValue(top) : null;
			if (topValue == null)
				return null;
			if (topValue instanceof Object[]) {
				if (!StringUtils.isNumeric(sub))
					return false;
				int index = Integer.valueOf(sub);
				Object[] array = (Object[]) topValue;
				if (index < 0 || index >= array.length)
					throw new IllegalArgumentException("Invalid index: " + sub);
				topValue = array[index];
			} else if (topValue instanceof Collection) {
				if (!StringUtils.isNumeric(sub))
					return false;
				int index = Integer.valueOf(sub);
				Collection<?> collection = (Collection<?>) topValue;
				if (index < 0 || index >= collection.size())
					throw new IllegalArgumentException("Invalid index: " + sub);
				Iterator<?> it = collection.iterator();
				int i = 0;
				while (it.hasNext()) {
					topValue = it.next();
					if (i == index)
						break;
					i++;
				}
			} else if (topValue instanceof Map) {
				if (!(sub.startsWith("'") && sub.endsWith("'") || sub.startsWith("\"") && sub.endsWith("\"")))
					throw new IllegalArgumentException("Invalid sub: " + sub);
				sub = sub.substring(1, sub.length() - 1);
				topValue = ((Map<?, ?>) topValue).get(sub);
			} else {
				throw new IllegalArgumentException("Not indexable: " + topValue);
			}
			if (index2 == paramName.length() - 1)
				return topValue;
			if (index3 == index2 + 1) {
				EntityBeanPropertySqlParameterSource source = new EntityBeanPropertySqlParameterSource(topValue);
				return source.getValue(paramName.substring(index3 + 1));
			}
		}
		if (index3 > 0) {
			String top = paramName.substring(0, index3);
			if (super.hasValue(top)) {
				Object topValue = getValue(top);
				if (topValue == null)
					return null;
				EntityBeanPropertySqlParameterSource source = new EntityBeanPropertySqlParameterSource(topValue);
				return source.getValue(paramName.substring(index3 + 1));
			}
		}
		return null;
	}

}
