package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

class ApplicationContextPropertiesCondition implements Condition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(ApplicationContextPropertiesConditional.class.getName()));
		String key = attributes.getString("key");
		String value = attributes.getString("value");
		boolean negated = attributes.getBoolean("negated");
		boolean matched = matches(key, value, negated);
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	public static boolean matches(String key, String value, boolean negated) {
		boolean matched = value.equals(AppInfo.getApplicationContextProperties().getProperty(key));
		if (negated)
			matched = !matched;
		return matched;
	}

}
