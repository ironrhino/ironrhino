package org.ironrhino.core.hibernate.convert;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;

@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {

	public static final String SEPARATOR = ",";

	@Override
	public String convertToDatabaseColumn(Set<String> set) {
		if (set == null)
			return null;
		if(set.isEmpty())
			return "";
		return StringUtils.join(set.iterator(), SEPARATOR);
	}

	@Override
	public Set<String> convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		Set<String> set = new LinkedHashSet<>();
		if (StringUtils.isNotBlank(string))
			set.addAll(Arrays.asList(string.split(SEPARATOR)));
		return set;
	}

}