package org.ironrhino.core.jdbc;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;

import javax.persistence.EnumType;

class JdbcHelper {
	static boolean isScalar(Class<?> type) {
		if(type.isEnum())
			return true;
		if (String.class == type)
			return true;
		if ((Boolean.TYPE == type) || (Boolean.class == type))
			return true;
		if ((Byte.TYPE == type) || (Byte.class == type))
			return true;
		if ((Short.TYPE == type) || (Short.class == type))
			return true;
		if ((Integer.TYPE == type) || (Integer.class == type))
			return true;
		if ((Long.TYPE == type) || (Long.class == type))
			return true;
		if ((Float.TYPE == type) || (Float.class == type))
			return true;
		if ((Double.TYPE == type) || (Double.class == type) || (Number.class == type))
			return true;
		if (BigDecimal.class == type)
			return true;
		if (java.sql.Date.class == type)
			return true;
		if (Time.class == type)
			return true;
		if ((Timestamp.class == type) || (java.util.Date.class == type))
			return true;
		return false;
	}

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
