package org.ironrhino.core.spring;

import java.beans.Introspector;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.util.ClassUtils;

public class NameGenerator extends AnnotationBeanNameGenerator {

	@Override
	protected String buildDefaultBeanName(BeanDefinition definition) {
		String shortClassName = ClassUtils.getShortName(definition
				.getBeanClassName());
		if (shortClassName.endsWith("Impl") && shortClassName.length() > 4)
			shortClassName = shortClassName.substring(0,
					shortClassName.length() - 4);
		return Introspector.decapitalize(shortClassName);
	}

}
