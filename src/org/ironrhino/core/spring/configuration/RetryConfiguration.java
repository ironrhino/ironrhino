package org.ironrhino.core.spring.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

@Configuration
public class RetryConfiguration implements BeanDefinitionRegistryPostProcessor {

	private static final String CLASS_NAME = "org.springframework.retry.annotation.RetryConfiguration";

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		if (!ClassUtils.isPresent(CLASS_NAME, getClass().getClassLoader())) {
			logger.warn("undetected class {}", CLASS_NAME);
			return;
		}
		try {
			Class<?> clazz = Class.forName(CLASS_NAME);
			RootBeanDefinition beanDefinition = new RootBeanDefinition(clazz);
			MutablePropertyValues propertyValues = new MutablePropertyValues();
			propertyValues.addPropertyValue("order", "-100");
			beanDefinition.setPropertyValues(propertyValues);
			registry.registerBeanDefinition(CLASS_NAME, beanDefinition);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}