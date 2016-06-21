package org.ironrhino.core.hibernate.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.apache.commons.lang3.StringUtils;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	public static final String SEPARATOR = ",";

	@Override
	public String convertToDatabaseColumn(List<String> list) {
		if (list == null)
			return null;
		if (list.isEmpty())
			return "";
		return StringUtils.join(list.iterator(), SEPARATOR);
	}

	@Override
	public List<String> convertToEntityAttribute(String string) {
		if (string == null)
			return null;
		List<String> list = new ArrayList<>();
		if (StringUtils.isNotBlank(string))
			list.addAll(Arrays.asList(string.split(SEPARATOR)));
		return list;
	}

}