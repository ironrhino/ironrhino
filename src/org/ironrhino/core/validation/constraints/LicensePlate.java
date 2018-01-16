package org.ironrhino.core.validation.constraints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.ironrhino.core.validation.validators.LicensePlateValidator;

@Target({ java.lang.annotation.ElementType.METHOD, java.lang.annotation.ElementType.FIELD,
		java.lang.annotation.ElementType.ANNOTATION_TYPE, java.lang.annotation.ElementType.CONSTRUCTOR,
		java.lang.annotation.ElementType.PARAMETER, java.lang.annotation.ElementType.TYPE_USE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { LicensePlateValidator.class })
public @interface LicensePlate {

	String message() default "{org.ironrhino.core.validation.validators.constraints.LicencePlate.message}";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}