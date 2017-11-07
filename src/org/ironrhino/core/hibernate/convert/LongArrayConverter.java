package org.ironrhino.core.hibernate.convert;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class LongArrayConverter extends AbstractArrayConverter<Long> implements AttributeConverter<Long[], String> {

	@Override
	protected Long convert(String s) {
		return Long.valueOf(s);
	}

}