package org.ironrhino.core.metadata;

import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target(LOCAL_VARIABLE)
@Retention(SOURCE)
public @interface NativeSql {
}
