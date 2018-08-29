package org.ironrhino.rest.client;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.configuration.AnnotationBeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

@Component
public class RestApiRegistryPostProcessor
		extends AnnotationBeanDefinitionRegistryPostProcessor<RestApi, RestApiFactoryBean> {

	@Override
	protected void processBeanDefinition(RestApi annotation, Class<?> annotatedClass,
			RootBeanDefinition beanDefinition) {
		beanDefinition.setPrimary(true);
		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addIndexedArgumentValue(0, annotatedClass);
		if (StringUtils.isNotBlank(annotation.restClient()))
			constructorArgumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference(annotation.restClient()));
		else if (StringUtils.isNotBlank(annotation.restTemplate()))
			constructorArgumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference(annotation.restTemplate()));
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
	}

	@Override
	protected String getExplicitBeanName(RestApi annotation) {
		return annotation.name();
	}

}