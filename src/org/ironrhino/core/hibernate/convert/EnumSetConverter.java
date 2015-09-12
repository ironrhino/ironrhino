package org.ironrhino.core.hibernate.convert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;

public abstract class EnumSetConverter<T extends Enum<T>> implements AttributeConverter<Set<T>, String> {

	public static final String SEPARATOR = ",";

	private Class<T> enumType;

	@SuppressWarnings("unchecked")
	public EnumSetConverter() {
		Class<T> clazz = (Class<T>) ReflectionUtils.getGenericClass(getClass());
		if (clazz != null)
			enumType = clazz;
	}

	@Override
	public String convertToDatabaseColumn(Set<T> set) {
		if (set == null)
			return null;
		List<String> names = new ArrayList<>();
		for (Enum<?> en : set)
			names.add(en.name());
		return StringUtils.join(names, SEPARATOR);
	}

	@Override
	public Set<T> convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		if (StringUtils.isBlank(string))
			return Collections.emptySet();
		String[] names = string.split(SEPARATOR);
		Set<T> set = new LinkedHashSet<>();
		for (String name : names)
			set.add(Enum.valueOf(enumType, name));
		return set;
	}

}