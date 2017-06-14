package org.ironrhino.core.hibernate.convert;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class StringArrayConverter extends AbstractArrayConverter<String>
		implements AttributeConverter<String[], String> {

	@Override
	protected String convert(String s) {
		return s;
	}

}