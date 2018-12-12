package org.ironrhino.rest.client;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.spring.configuration.AnnotationBeanDefinitionRegistryPostProcessor;
import org.ironrhino.core.util.ClassScanner;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Component
public class RestApiRegistryPostProcessor
		extends AnnotationBeanDefinitionRegistryPostProcessor<RestApi, RestApiFactoryBean> {

	@Override
	protected void processBeanDefinition(RestApi annotation, Class<?> annotatedClass,
			RootBeanDefinition beanDefinition) {
		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
		constructorArgumentValues.addIndexedArgumentValue(0, annotatedClass);
		if (StringUtils.isNotBlank(annotation.restClient()))
			constructorArgumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference(annotation.restClient()));
		else if (StringUtils.isNotBlank(annotation.restTemplate()))
			constructorArgumentValues.addIndexedArgumentValue(1, new RuntimeBeanReference(annotation.restTemplate()));
		beanDefinition.setConstructorArgumentValues(constructorArgumentValues);
	}

	@Override
	protected boolean shouldSkip(Class<?> annotatedClass) {
		Class<?>[] annotatedClasses = getAnnotatedClasses();
		if (annotatedClasses != null) {
			for (Class<?> c : annotatedClasses) {
				if (c.equals(annotatedClass))
					return false;
			}
		}
		Collection<Class<?>> assignables = ClassScanner.scanAssignable(annotatedClass.getPackage().getName(),
				annotatedClass);
		for (Class<?> c : assignables) {
			if (!c.isInterface() && AnnotationUtils.findAnnotation(c, Controller.class) != null) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected String getExplicitBeanName(RestApi annotation) {
		return annotation.name();
	}

}