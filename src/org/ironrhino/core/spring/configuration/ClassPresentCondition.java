package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;

public class ClassPresentCondition implements Condition {

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ClassPresentConditional.class
						.getName());
		return matches((String) attributes.get("value"),
				(Boolean) attributes.get("negated"));
	}

	public static boolean matches(String value, boolean negated) {
		boolean b = ClassUtils.isPresent(value,
				ClassPresentCondition.class.getClassLoader());
		return b && !negated || !b && negated;
	}

}
