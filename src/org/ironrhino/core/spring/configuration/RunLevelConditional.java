package org.ironrhino.core.spring.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.ironrhino.core.util.AppInfo.RunLevel;
import org.springframework.context.annotation.Conditional;

@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Conditional(RunLevelCondition.class)
public @interface RunLevelConditional {

	public RunLevel value() default RunLevel.NORMAL;
	
	public boolean negated() default false;

}