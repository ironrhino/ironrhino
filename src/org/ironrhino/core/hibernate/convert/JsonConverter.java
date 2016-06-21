package org.ironrhino.core.hibernate.convert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;

public abstract class JsonConverter<T> implements AttributeConverter<T, String> {

	private ParameterizedType type;

	public JsonConverter() {
		ParameterizedType pramType = (ParameterizedType) getClass().getGenericSuperclass();
		Type[] params = pramType.getActualTypeArguments();
		type = (ParameterizedType) params[0];
	}

	@Override
	public String convertToDatabaseColumn(T obj) {
		if (obj == null)
			return null;
		if (obj instanceof Collection && ((Collection<?>) obj).isEmpty()
				|| obj instanceof Map && ((Map<?, ?>) obj).isEmpty())
			return "";
		return JsonUtils.toJson(obj);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		if (StringUtils.isEmpty(string)) {
			if (List.class.isAssignableFrom((Class<?>) type.getRawType())) {
				return (T) new ArrayList<>();
			} else if (Set.class.isAssignableFrom((Class<?>) type.getRawType())) {
				return (T) new LinkedHashSet<>();
			} else if (Map.class.isAssignableFrom((Class<?>) type.getRawType())) {
				return (T) new LinkedHashMap<>();
			}
		}
		try {
			return (T) JsonUtils.fromJson(string, type);
		} catch (Exception e) {
			throw new IllegalArgumentException(string + " is not valid json ", e);
		}
	}

}