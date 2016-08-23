package org.ironrhino.core.hibernate;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.hibernate.annotations.ValueGenerationType;

@ValueGenerationType(generatedBy = CreationUserGeneration.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface CreationUser {

}
