package org.ironrhino.core.hibernate.convert;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class IntegerArrayConverter extends AbstractArrayConverter<Integer>
		implements AttributeConverter<Integer[], String> {

	@Override
	protected Integer convert(String s) {
		return Integer.valueOf(s);
	}

}