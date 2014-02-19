package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.RunLevel;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class RunLevelCondition implements Condition {

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(RunLevelConditional.class.getName());
		return matches((RunLevel) attributes.get("value"),
				(Boolean) attributes.get("negated"));
	}

	public static boolean matches(RunLevel runLevel, boolean negated) {
		boolean b = AppInfo.getRunLevel().compareTo(runLevel) >= 0;
		return b && !negated || !b && negated;
	}

	public static boolean matches(String s, boolean negated) {
		return matches(RunLevel.valueOf(s), negated);
	}

}
