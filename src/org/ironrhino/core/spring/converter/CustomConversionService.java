package org.ironrhino.core.spring.converter;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.function.SingletonSupplier;

public class CustomConversionService extends DefaultConversionService {

	private static SingletonSupplier<CustomConversionService> singletonSupplier = SingletonSupplier
			.of(CustomConversionService::new);

	public CustomConversionService() {
		super();
		addConverter(new DateConverter());
		addConverter(new LocalDateConverter());
		addConverter(new LocalDateTimeConverter());
		addConverter(new LocalTimeConverter());
		addConverter(new YearMonthConverter());
		addConverter(new DurationConverter());
		addConverter(new EnumToEnumConverter());
		addConverter(new SerializableToSerializableConverter());
		addConverter(new StringToMapConverter());
		addConverter(new MapToStringConverter());
	}

	public static CustomConversionService getSharedInstance() {
		return singletonSupplier.obtain();
	}

}
