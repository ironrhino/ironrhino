package org.ironrhino.core.spring.configuration;

import org.springframework.util.ClassUtils;

public class ClassPresentCondition extends SimpleCondition<ClassPresentConditional> {

	@Override
	public boolean matches(ClassPresentConditional annotation) {
		return matches(annotation.value(), annotation.negated());
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
