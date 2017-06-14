package org.ironrhino.core.hibernate.convert;

import java.lang.reflect.Array;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.ReflectionUtils;

@SuppressWarnings("unchecked")
public abstract class AbstractArrayConverter<T> {

	public static final String SEPARATOR = AbstractCollectionConverter.SEPARATOR;

	private Class<T> clazz;

	public AbstractArrayConverter() {
		clazz = (Class<T>) ReflectionUtils.getGenericClass(getClass());
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
			return (T[]) Array.newInstance(clazz, 0);
		String[] arr = string.split(SEPARATOR + "\\s*");
		T[] array = (T[]) Array.newInstance(clazz, arr.length);
		for (int i = 0; i < arr.length; i++)
			array[i] = convert(arr[i]);
		return array;
	}

	protected abstract T convert(String s);

}