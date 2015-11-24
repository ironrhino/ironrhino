package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.ironrhino.core.util.AppInfo;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

class ApplicationContextPropertiesCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ApplicationContextPropertiesConditional.class.getName());
		String key = (String) attributes.get("key");
		String value = (String) attributes.get("value");
		boolean negated = (Boolean) attributes.get("negated");
		boolean matches = value.equals(AppInfo.getApplicationContextProperties().getProperty(key));
		return matches && !negated || !matches && negated;
	}

}
