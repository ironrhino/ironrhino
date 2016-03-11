package org.ironrhino.core.spring.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;


@SuppressWarnings({"unchecked", "rawtypes"})
public final class IntegerToEnumConverterFactory implements ConverterFactory<Integer, Enum> {

	@Override
	public <T extends Enum> Converter<Integer, T> getConverter(Class<T> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		if (enumType == null) {
			throw new IllegalArgumentException(
					"The target type " + targetType.getName() + " does not refer to an enum");
		}
		return new IntegerToEnum(enumType);
	}


	private class IntegerToEnum<T extends Enum> implements Converter<Integer, T> {

		private final Class<T> enumType;

		public IntegerToEnum(Class<T> enumType) {
			this.enumType = enumType;
		}

		@Override
		public T convert(Integer source) {
			if (source == null) {
				return null;
			}
			return enumType.getEnumConstants()[source];
		}
	}

}
