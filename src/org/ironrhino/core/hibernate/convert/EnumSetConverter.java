package org.ironrhino.core.hibernate.convert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.AttributeConverter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.GenericTypeResolver;

public abstract class EnumSetConverter<T extends Enum<T>> implements AttributeConverter<Set<T>, String> {

	public static final String SEPARATOR = AbstractCollectionConverter.SEPARATOR;

	private final Class<T> enumType;

	@SuppressWarnings("unchecked")
	public EnumSetConverter() {
		enumType = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), EnumSetConverter.class);
	}

	@Override
	public String convertToDatabaseColumn(Set<T> set) {
		if (set == null)
			return null;
		if (set.isEmpty())
			return "";
		List<String> names = new ArrayList<>();
		for (Enum<?> en : set)
			names.add(en.name());
		return String.join(SEPARATOR, names);
	}

	@Override
	public Set<T> convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		if (StringUtils.isBlank(string))
			return new LinkedHashSet<>();
		String[] names = string.split(SEPARATOR);
		Set<T> set = new LinkedHashSet<>();
		for (String name : names)
			set.add(Enum.valueOf(enumType, name));
		return set;
	}

}