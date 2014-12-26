package org.ironrhino.core.spring.configuration;

import java.util.Collections;

import org.ironrhino.core.servlet.HttpErrorHandler;
import org.ironrhino.core.spring.converter.DateConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySources({
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.${STAGE}.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.${STAGE}.properties") })
@ComponentScan(excludeFilters = @Filter(value = HttpErrorHandler.class, type = FilterType.ASSIGNABLE_TYPE))
@EnableAspectJAutoProxy
public class CommonConfiguration {

	@Bean
	public PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ConversionServiceFactoryBean conversionService() {
		ConversionServiceFactoryBean cfb = new ConversionServiceFactoryBean();
		cfb.setConverters(Collections.singleton(new DateConverter()));
		return cfb;
	}
}
