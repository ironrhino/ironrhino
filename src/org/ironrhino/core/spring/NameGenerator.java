package org.ironrhino.core.spring;

import java.beans.Introspector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

public class NameGenerator extends AnnotationBeanNameGenerator {

	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		return buildDefaultBeanName(definition.getBeanClassName());
	}

	public static String buildDefaultBeanName(String beanClassName) {
		if (beanClassName.startsWith("org.ironrhino") && beanClassName.endsWith("Configuration")) {
			try {
				Class<?> clazz = Class.forName(beanClassName);
				if (clazz.isAnnotationPresent(Configuration.class))
					return beanClassName;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}
		String shortClassName = ClassUtils.getShortName(beanClassName);
		if (shortClassName.endsWith("Impl") && shortClassName.length() > 4)
			shortClassName = shortClassName.substring(0, shortClassName.length() - 4);
		return Introspector.decapitalize(shortClassName);
	}

}
