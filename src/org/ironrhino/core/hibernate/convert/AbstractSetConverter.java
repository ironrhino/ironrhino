package org.ironrhino.core.hibernate.convert;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.AttributeConverter;

public abstract class AbstractSetConverter<T> extends AbstractCollectionConverter<T>
		implements AttributeConverter<Set<T>, String> {

	@Override
	public String convertToDatabaseColumn(Set<T> list) {
		return doConvertToDatabaseColumn(list);
	}

	@Override
	public Set<T> convertToEntityAttribute(String string) {
		return (Set<T>) super.doConvertToEntityAttribute(string);
	}

	@Override
	protected Collection<T> collection() {
		return new LinkedHashSet<>();
	}

	protected abstract T convert(String s);

}