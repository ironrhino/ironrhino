package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.spring.converter.CustomConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;

@Order(0)
@Configuration
@PropertySources({
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.${STAGE}.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.${STAGE}.properties") })
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CommonConfiguration {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ConversionService conversionService() {
		return new CustomConversionService();
	}
	
	@Bean
    @Scope(BeanDefinition.SCOPE_PROTOTYPE)
    public Logger createLogger(InjectionPoint injectionPoint) {
		return LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass());
    }
}
