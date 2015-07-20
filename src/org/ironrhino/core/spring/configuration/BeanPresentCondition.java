package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Order(Ordered.LOWEST_PRECEDENCE)
class BeanPresentCondition implements ConfigurationCondition {

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(BeanPresentConditional.class.getName());
		String name = (String) attributes.get("value");
		boolean negated = (Boolean) attributes.get("negated");
		BeanDefinitionRegistry bdr = ctx.getRegistry();
		boolean matched = bdr.containsBeanDefinition(name);
		if (negated)
			matched = !matched;
		return matched;
	}

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

}