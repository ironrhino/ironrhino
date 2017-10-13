package org.ironrhino.core.jdbc;

import java.lang.annotation.Annotation;

import javax.persistence.EnumType;

class JdbcHelper {

	static Object convertEnum(Object arg, Annotation[] paramAnnotations) {
		Enum<?> en = (Enum<?>) arg;
		for (Annotation ann : paramAnnotations) {
			if (ann instanceof Enumerated) {
				arg = (((Enumerated) ann).value() == EnumType.ORDINAL) ? en.ordinal() : en.name();
				break;
			}
			if (ann instanceof javax.persistence.Enumerated) {
				arg = (((javax.persistence.Enumerated) ann).value() == EnumType.ORDINAL) ? en.ordinal() : en.name();
				break;
			}
		}
		return arg;
	}

}
