package org.ironrhino.core.spring.configuration;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ResourcePresentCondition extends SimpleCondition<ResourcePresentConditional> {

	private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver(
			ResourcePresentCondition.class.getClassLoader());

	@Override
	public boolean matches(ResourcePresentConditional annotation) {
		return matches(annotation.value(), annotation.negated());
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
