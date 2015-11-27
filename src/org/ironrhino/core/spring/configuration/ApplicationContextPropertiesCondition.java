package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

class ApplicationContextPropertiesCondition implements Condition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ApplicationContextPropertiesConditional.class.getName());
		String key = (String) attributes.get("key");
		String value = (String) attributes.get("value");
		boolean negated = (Boolean) attributes.get("negated");
		boolean matched = value.equals(AppInfo.getApplicationContextProperties().getProperty(key));
		if (negated)
			matched = !matched;
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

}
