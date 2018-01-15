package org.ironrhino.sample.crud;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.FIELD,
		java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.CONSTRUCTOR,
		java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { IdentityCheckValidator.class })
public @interface IdentityCheck {

	String message() default "{org.ironrhino.sample.crud.IdentityCheck.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}