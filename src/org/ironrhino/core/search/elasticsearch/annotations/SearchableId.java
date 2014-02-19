package org.ironrhino.core.search.elasticsearch.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RUNTIME)
public @interface SearchableId {

	String type() default "";

	String index_name() default "";

	float boost() default 1.0f;

	Store store() default Store.NA;

	Index index() default Index.NA;

	String analyzer() default "";

	String index_analyzer() default "";

	String search_analyzer() default "";

	String format() default "";

	boolean include_in_all() default true;

	String null_value() default "";

	TermVector term_vector() default TermVector.NA;

	boolean omit_norms() default false;

	boolean omit_term_freq_and_positions() default false;

	boolean ignore_malformed() default false;

}
