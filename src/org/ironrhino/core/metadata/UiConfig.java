package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({ METHOD, FIELD })
@Retention(RUNTIME)
public @interface UiConfig {

	String DEFAULT_TYPE = "input";

	String DEFAULT_INPUT_TYPE = "text";

	String DEFAULT_LIST_KEY = "id";

	String DEFAULT_LIST_VALUE = "name";

	String alias() default "";

	String id() default "";

	String type() default DEFAULT_TYPE; // input,textarea,enum,select,multiselect,checkbox,listpick,dictionary,schema...

	String inputType() default DEFAULT_INPUT_TYPE; // text,password,email,number...

	int maxlength() default 0;

	String regex() default "";
	
	boolean trim() default true;

	String cssClass() default "";

	String thCssClass() default "";

	int displayOrder() default Integer.MAX_VALUE;

	boolean required() default false;

	boolean unique() default false;

	Readonly readonly() default @Readonly;

	boolean hidden() default false;

	Hidden hiddenInList() default @Hidden;

	Hidden hiddenInInput() default @Hidden;

	Hidden hiddenInView() default @Hidden;

	boolean shownInPick() default false;

	boolean searchable() default false;

	String template() default "";

	String listTemplate() default "";

	String viewTemplate() default "";

	String width() default "";

	String dynamicAttributes() default ""; // json map

	String cellDynamicAttributes() default ""; // json map

	boolean excludeIfNotEdited() default false;

	String listKey() default DEFAULT_LIST_KEY;

	String listValue() default DEFAULT_LIST_VALUE;

	String cellEdit() default "";

	String optionsExpression() default ""; // for select,multiselect

	String pickUrl() default "";// for listpick

	String templateName() default ""; // for dictionary,schema

	boolean excludedFromLike() default false;

	boolean excludedFromCriteria() default false;

	boolean excludedFromOrdering() default false;

}