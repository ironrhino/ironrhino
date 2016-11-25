package org.ironrhino.core.spring.configuration;

import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.RunLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

public class RunLevelCondition implements Condition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		RunLevelConditional annotation = AnnotationUtils.getAnnotation(metadata, RunLevelConditional.class);
		boolean matched = matches(annotation.value(), annotation.negated());
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	public static boolean matches(RunLevel runLevel, boolean negated) {
		boolean matched = AppInfo.getRunLevel().compareTo(runLevel) >= 0;
		return matched && !negated || !matched && negated;
	}

	public static boolean matches(String s, boolean negated) {
		return matches(RunLevel.valueOf(s), negated);
	}

}
