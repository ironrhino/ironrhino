package org.ironrhino.core.hibernate.convert;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;

@SuppressWarnings("unchecked")
public abstract class EnumArrayConverter<T extends Enum<T>> {

	public static final String SEPARATOR = AbstractCollectionConverter.SEPARATOR;

	private Class<T> enumType;

	public EnumArrayConverter() {
		enumType = (Class<T>) ReflectionUtils.getGenericClass(getClass(), EnumArrayConverter.class);
	}

	public String convertToDatabaseColumn(T[] array) {
		if (array == null)
			return null;
		if (array.length == 0)
			return "";
		List<String> names = new ArrayList<>();
		for (T en : array)
			names.add(en.name());
		return StringUtils.join(names, SEPARATOR);
	}

	public T[] convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		if (string.isEmpty())
			return (T[]) Array.newInstance(enumType, 0);
		String[] arr = string.split(SEPARATOR + "\\s*");
		T[] array = (T[]) Array.newInstance(enumType, arr.length);
		for (int i = 0; i < arr.length; i++)
			array[i] = Enum.valueOf(enumType, arr[i]);
		return array;
	}

}