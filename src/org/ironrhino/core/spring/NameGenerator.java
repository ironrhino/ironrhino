package org.ironrhino.core.spring;

import java.beans.Introspector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.util.ClassUtils;

public class NameGenerator extends AnnotationBeanNameGenerator {

	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String className = definition.getBeanClassName();
		if (className == null)
			return super.buildDefaultBeanName(definition);
		return buildDefaultBeanName(className);
	}

	public static String buildDefaultBeanName(String beanClassName) {
		String shortClassName = ClassUtils.getShortName(beanClassName);
		if (beanClassName.startsWith("org.ironrhino")) {
			if (shortClassName.startsWith("Default") && shortClassName.length() > 7)
				return Introspector.decapitalize(shortClassName.substring(7));
			if (beanClassName.endsWith("Configuration")) {
				try {
					Class<?> clazz = Class.forName(beanClassName);
					if (clazz.isAnnotationPresent(Configuration.class)) {
						Role role = clazz.getAnnotation(Role.class);
						if (role != null && role.value() == BeanDefinition.ROLE_INFRASTRUCTURE)
							return beanClassName;
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		if (shortClassName.endsWith("Impl") && shortClassName.length() > 4)
			shortClassName = shortClassName.substring(0, shortClassName.length() - 4);
		return Introspector.decapitalize(shortClassName);
	}

}
