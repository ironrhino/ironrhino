package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

@Order(Ordered.LOWEST_PRECEDENCE)
class BeanPresentCondition implements ConfigurationCondition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		BeanPresentConditional annotation = AnnotationUtils.getAnnotation(metadata,
				BeanPresentConditional.class);
		String name = annotation.value();
		boolean negated = annotation.negated();
		BeanDefinitionRegistry bdr = ctx.getRegistry();
		boolean matched = bdr.containsBeanDefinition(name);
		if (!matched && name.indexOf('.') > 0) {
			for (String beanName : bdr.getBeanDefinitionNames()) {
				try {
					Class<?> beanclazz = Class.forName(bdr.getBeanDefinition(beanName).getBeanClassName());
					if (Class.forName(name).isAssignableFrom(beanclazz)) {
						matched = true;
						break;
					}
				} catch (ClassNotFoundException e) {
					continue;
				}
			}
		}
		if (negated)
			matched = !matched;
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

}