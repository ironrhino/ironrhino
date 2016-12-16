package org.ironrhino.core.spring.converter;

import org.springframework.core.convert.support.DefaultConversionService;

public class CustomConversionService extends DefaultConversionService {

	private static volatile CustomConversionService sharedInstance;

	public CustomConversionService() {
		super();
		addConverter(new DateConverter());
		addConverter(new EnumToEnumConverter());
		addConverter(new SerializableToSerializableConverter());
		addConverter(new StringToMapConverter());
		addConverter(new MapToStringConverter());
	}

	public static CustomConversionService getSharedInstance() {
		if (sharedInstance == null) {
			synchronized (DefaultConversionService.class) {
				if (sharedInstance == null) {
					sharedInstance = new CustomConversionService();
				}
			}
		}
		return sharedInstance;
	}

}
