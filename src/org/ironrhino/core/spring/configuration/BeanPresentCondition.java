package org.ironrhino.core.spring.configuration;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.AnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
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
		BeanPresentConditional annotation = AnnotationUtils.getAnnotation(metadata, BeanPresentConditional.class);
		String name = annotation.name();
		if (StringUtils.isBlank(name) && StringUtils.isNotBlank(annotation.value()))
			name = annotation.value();
		Class<?> type = annotation.type();
		if (type == void.class)
			type = null;
		if (StringUtils.isBlank(name) && type == null)
			return false;
		ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
		boolean matched = type == null ? beanFactory.containsBean(name)
				: beanFactory.getBeanNamesForType(type).length > 0;
		if (annotation.negated())
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