package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ResourcePresentCondition implements Condition {

	private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(
			ResourcePresentCondition.class.getClassLoader());

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata.getAnnotationAttributes(ResourcePresentConditional.class.getName());
		return matches((String[]) attributes.get("value"), (Boolean) attributes.get("negated"));
	}

	public static boolean matches(String value[], boolean negated) {
		boolean matches = true;
		for (String val : value) {
			if (!exists(val))
				matches = false;
		}
		return matches && !negated || !matches && negated;
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
