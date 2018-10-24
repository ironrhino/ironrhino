package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.validation.MessageInterpolator;
import javax.validation.Validator;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.ironrhino.core.spring.converter.CustomConversionService;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.annotation.Role;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import lombok.extern.slf4j.Slf4j;

@Order(0)
@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
@PropertySources({
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.yaml", factory = YamlPropertySourceFactory.class),
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.${STAGE}.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "classpath:resources/spring/applicationContext.${STAGE}.yaml", factory = YamlPropertySourceFactory.class),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.yaml", factory = YamlPropertySourceFactory.class),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.${STAGE}.properties"),
		@PropertySource(ignoreResourceNotFound = true, value = "file:${app.home}/conf/applicationContext.${STAGE}.yaml", factory = YamlPropertySourceFactory.class) })
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Slf4j
public class CommonConfiguration {

	public static final String GLOBAL_MESSAGES_PATTERN = "resources/i18n/**/*.properties";

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public Logger createLogger(InjectionPoint injectionPoint) {
		return LoggerFactory.getLogger(injectionPoint.getMember().getDeclaringClass());
	}

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public ConversionService conversionService() {
		return new CustomConversionService();
	}

	@Bean
	public LocalValidatorFactoryBean validatorFactory() {
		LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
		boolean caching = AppInfo.getStage() != Stage.DEVELOPMENT;
		MessageInterpolator messageInterpolator = new ResourceBundleMessageInterpolator(
				new MessageSourceResourceBundleLocator(messageSource(true)), caching);
		validatorFactoryBean.setMessageInterpolator(messageInterpolator);
		return validatorFactoryBean;
	}

	@Bean
	public static MethodValidationPostProcessor methodValidationPostProcessor(@Lazy Validator validator) {
		MethodValidationPostProcessor postProcessor = new MethodValidationPostProcessor();
		postProcessor.setValidator(validator);
		postProcessor.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return postProcessor;
	}

	@Bean
	public MessageSource messageSource() {
		return messageSource(false);
	}

	protected MessageSource messageSource(boolean forValidation) {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		boolean caching = AppInfo.getStage() != Stage.DEVELOPMENT;
		messageSource.setCacheSeconds(caching ? -1 : 0);
		messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
		messageSource.setBasenames(getMessageSourceBasenames(forValidation));
		log.info("Loading {} messages from {}", forValidation ? "validation" : "global",
				messageSource.getBasenameSet());
		return messageSource;
	}

	/*
	 * order by priority descend
	 */
	protected String[] getMessageSourceBasenames(boolean forValidation) {

		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		String searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + GLOBAL_MESSAGES_PATTERN;
		Map<String, String> fileMessageBunldes = new TreeMap<>();
		Map<String, String> jarMessageBunldes = new TreeMap<>();
		try {
			Resource[] resources = resourcePatternResolver.getResources(searchPath);
			for (Resource res : resources) {
				Map<String, String> messageBunldes = res.getURI().getScheme().equals("file") ? fileMessageBunldes
						: jarMessageBunldes;
				String name = res.getURI().toString();
				String source = name.substring(0, name.indexOf("/resources/i18n/"));
				name = name.substring(name.indexOf("resources/i18n/"));
				name = org.ironrhino.core.util.StringUtils.trimLocale(name.substring(0, name.lastIndexOf('.')));
				if (messageBunldes.containsKey(name) && !source.equals(messageBunldes.get(name))) {
					log.warn("Global messages " + name + " ignored from " + source + ", will load from "
							+ messageBunldes.get(name));
				} else {
					messageBunldes.put(name, source);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<String> basenames = new ArrayList<>();
		String validationMessages = "/ValidationMessages";
		if (forValidation) {
			basenames.add("org/ironrhino/core/validation" + validationMessages);
			for (String name : jarMessageBunldes.keySet())
				if (name.endsWith(validationMessages))
					basenames.add(name);
			for (String name : fileMessageBunldes.keySet())
				if (name.endsWith(validationMessages))
					basenames.add(name);
		} else {
			for (String name : jarMessageBunldes.keySet())
				if (!name.endsWith(validationMessages))
					basenames.add(name);
			String commonPrefix = "resources/i18n/common/";
			basenames.sort((a, b) -> {
				if (a.startsWith(commonPrefix))
					return b.startsWith(commonPrefix) ? a.compareTo(b) : -1;
				return a.compareTo(b);
			});
			for (String name : fileMessageBunldes.keySet())
				if (!name.endsWith(validationMessages))
					basenames.add(name);
		}
		Collections.reverse(basenames);
		return basenames.stream().map(s -> ResourceLoader.CLASSPATH_URL_PREFIX + s).collect(Collectors.toList())
				.toArray(new String[basenames.size()]);
	}

}
