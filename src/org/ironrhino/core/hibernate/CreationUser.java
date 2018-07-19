package org.ironrhino.core.hibernate;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.ValueGenerationType;

@ValueGenerationType(generatedBy = CreationUserGeneration.class)
@Target({ FIELD, METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface CreationUser {

}
