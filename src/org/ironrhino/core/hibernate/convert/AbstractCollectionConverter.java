package org.ironrhino.core.hibernate.convert;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractCollectionConverter<T> {

	public static final String SEPARATOR = ",";

	protected static <T> String doConvertToDatabaseColumn(Collection<T> collection) {
		if (collection == null)
			return null;
		if (collection.isEmpty())
			return "";
		return StringUtils.join(collection.iterator(), SEPARATOR);
	}

	protected Collection<T> doConvertToEntityAttribute(String string) {
		if (string == null)
			return null;
		String[] arr = string.split(SEPARATOR + "\\s*");
		Collection<T> collection = collection();
		for (String s : arr)
			if (StringUtils.isNotBlank(s))
				collection.add(convert(s));
		return collection;
	}

	protected abstract Collection<T> collection();

	protected abstract T convert(String s);

}