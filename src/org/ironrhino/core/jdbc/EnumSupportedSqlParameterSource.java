package org.ironrhino.core.jdbc;

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;

public class EnumSupportedSqlParameterSource extends BeanPropertySqlParameterSource {

	private boolean useNamed;

	public EnumSupportedSqlParameterSource(Object object, boolean useNamed) {
		super(object);
		this.useNamed = useNamed;
	}

	@Override
	public Object getValue(String name) throws IllegalArgumentException {
		Object value = super.getValue(name);
		if (value instanceof Enum) {
			Enum<?> en = ((Enum<?>) value);
			return useNamed ? en.name() : en.ordinal();
		}
		return value;
	}

}
