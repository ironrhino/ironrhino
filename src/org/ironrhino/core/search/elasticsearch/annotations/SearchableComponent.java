package org.ironrhino.core.search.elasticsearch.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SearchableComponent {

	String refAlias() default "";

	boolean override() default true;

	int maxDepth() default 1;

	String prefix() default "";

	String converter() default "";

	String accessor() default "";
}
