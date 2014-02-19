package org.ironrhino.core.spring.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.util.AppInfo.Stage;
import org.springframework.context.annotation.Conditional;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(StageCondition.class)
public @interface StageConditional {

	public Stage value() default Stage.PRODUCTION;
	
	public boolean negated() default false;

}