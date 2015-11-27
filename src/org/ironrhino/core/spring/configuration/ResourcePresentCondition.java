package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

public class ResourcePresentCondition implements Condition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(
			ResourcePresentCondition.class.getClassLoader());

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ResourcePresentConditional.class.getName());
		boolean matched = matches((String[]) attributes.get("value"), (Boolean) attributes.get("negated"));
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	public static boolean matches(String value[], boolean negated) {
		boolean matched = true;
		for (String val : value) {
			if (!exists(val))
				matched = false;
		}
		return matched && !negated || !matched && negated;
	}

	private static boolean exists(String resource) {
		try {
			Resource[] resources = resourcePatternResolver.getResources(resource);
			for (Resource r : resources)
				if (r.exists()) {
					return true;
				}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
