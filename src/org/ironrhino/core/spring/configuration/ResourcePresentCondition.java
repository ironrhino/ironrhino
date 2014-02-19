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

	private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(ResourcePresentConditional.class
						.getName());
		return matches((String) attributes.get("value"),
				(Boolean) attributes.get("negated"));
	}

	public static boolean matches(String value, boolean negated) {
		boolean b = false;
		try {
			Resource[] resources = resourcePatternResolver.getResources(value);
			for (Resource r : resources)
				if (r.exists()) {
					b = true;
					break;
				}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return b && !negated || !b && negated;
	}

}
