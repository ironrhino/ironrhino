package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.util.ClassUtils;

public class ClassPresentCondition implements Condition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ClassPresentConditional.class.getName());
		boolean matched = matches((String[]) attributes.get("value"), (Boolean) attributes.get("negated"));
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	public static boolean matches(String[] value, boolean negated) {
		boolean matched = true;
		for (String val : value) {
			if (!ClassUtils.isPresent(val, ClassPresentCondition.class.getClassLoader()))
				matched = false;
		}
		return matched && !negated || !matched && negated;
	}

}
