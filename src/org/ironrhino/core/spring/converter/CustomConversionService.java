package org.ironrhino.core.spring.converter;

import org.springframework.core.convert.support.DefaultConversionService;

public class CustomConversionService extends DefaultConversionService {

	private static volatile CustomConversionService sharedInstance;

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
		CustomConversionService cs = sharedInstance;
		if (cs == null) {
			synchronized (DefaultConversionService.class) {
				if ((cs = sharedInstance) == null)
					sharedInstance = cs = new CustomConversionService();
			}
		}
		return cs;
	}

}
