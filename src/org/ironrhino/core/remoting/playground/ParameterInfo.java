package org.ironrhino.core.remoting.playground;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.ironrhino.core.model.Displayable;
import org.ironrhino.core.util.TypeUtils;

import lombok.Data;

@Data
public class ParameterInfo implements Serializable {

	private static final long serialVersionUID = 1311380262742909281L;

	private Type type;

	private String name;

	private String sample;

	public boolean isRequired() {
		return type instanceof Class && ((Class<?>) type).isPrimitive();
	}

	public boolean isIntegralNumeric() {
		if (type instanceof Class) {
			return TypeUtils.isIntegralNumeric((Class<?>) type);
		}
		return false;
	}

	public boolean isDecimalNumeric() {
		if (type instanceof Class) {
			return TypeUtils.isDecimalNumeric((Class<?>) type);
		}
		return false;
	}

	public boolean isBool() {
		if (type instanceof Class) {
			Class<?> c = (Class<?>) type;
			return c == Boolean.class || c == boolean.class;
		}
		return false;
	}

	public boolean isEnum() {
		if (type instanceof Class) {
			Class<?> c = (Class<?>) type;
			return Enum.class.isAssignableFrom(c);
		}
		return false;
	}
	
	public boolean isDisplayable() {
		if (type instanceof Class) {
			Class<?> c = (Class<?>) type;
			return Displayable.class.isAssignableFrom(c);
		}
		return false;
	}

	public boolean isTemporal() {
		if (type instanceof Class) {
			return TypeUtils.isTemporal((Class<?>) type);
		}
		return false;
	}

	public boolean isMultiline() {
		return sample != null && sample.split("\n").length > 1;
	}

	@Override
	public String toString() {
		return ((type instanceof Class) ? ((Class<?>) type).getCanonicalName() : type.toString()) + " " + name;
	}

}
