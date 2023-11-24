package org.ironrhino.core.hibernate.convert;

import java.lang.reflect.Array;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.GenericTypeResolver;

@SuppressWarnings("unchecked")
public abstract class AbstractArrayConverter<T> {

	public static final String SEPARATOR = AbstractCollectionConverter.SEPARATOR;

	private final Class<T> componentType;

	public AbstractArrayConverter() {
		componentType = (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), AbstractArrayConverter.class);
	}

	public String convertToDatabaseColumn(T[] array) {
		if (array == null)
			return null;
		if (array.length == 0)
			return "";
		return StringUtils.join(array, SEPARATOR);
	}

	public T[] convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		if (string.isEmpty())
			return (T[]) Array.newInstance(componentType, 0);
		String[] arr = string.split(SEPARATOR + "\\s*");
		T[] array = (T[]) Array.newInstance(componentType, arr.length);
		for (int i = 0; i < arr.length; i++)
			array[i] = convert(arr[i]);
		return array;
	}

	protected abstract T convert(String s);

}