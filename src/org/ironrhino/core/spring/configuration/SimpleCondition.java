package org.ironrhino.core.spring.configuration;

import java.lang.annotation.Annotation;

import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SimpleCondition<T extends Annotation> implements Condition {

	private Class<T> annotationClass;

	@SuppressWarnings("unchecked")
	public SimpleCondition() {
		annotationClass = (Class<T>) ReflectionUtils.getGenericClass(getClass(), SimpleCondition.class);
	}

	public Class<T> getAnnotationClass() {
		return annotationClass;
	}

	@Override
	public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata metadata) {
		boolean matched = matches(metadata);
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			log.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	public boolean matches(AnnotatedTypeMetadata metadata) {
		T[] annotations = AnnotationUtils.getAnnotationsByType(metadata, annotationClass);
		boolean matched = true;
		for (T annotation : annotations) {
			if (!matches(annotation)) {
				matched = false;
				break;
			}
		}
		return matched;
	}

	public abstract boolean matches(T annotation);

}
