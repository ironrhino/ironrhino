package org.ironrhino.core.hibernate.convert;

import javax.persistence.Converter;

@Converter(autoApply = true)
public class IntegerSetConverter extends AbstractSetConverter<Integer> {

	@Override
	protected Integer convert(String s) {
		return Integer.valueOf(s);
	}

}