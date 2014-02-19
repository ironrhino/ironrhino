package org.ironrhino.core.spring.configuration;

import java.util.Map;

import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class StageCondition implements Condition {

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(StageConditional.class.getName());
		return matches((Stage) attributes.get("value"),
				(Boolean) attributes.get("negated"));
	}

	public static boolean matches(Stage stage, boolean negated) {
		boolean b = AppInfo.getStage() == stage;
		return b && !negated || !b && negated;
	}

	public static boolean matches(String s, boolean negated) {
		return matches(Stage.valueOf(s), negated);
	}

}
