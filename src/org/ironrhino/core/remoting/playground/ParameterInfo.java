package org.ironrhino.core.remoting.playground;

import java.io.Serializable;
import java.lang.reflect.Type;

import lombok.Data;

@Data
public class ParameterInfo implements Serializable {

	private static final long serialVersionUID = 1311380262742909281L;

	private Type type;

	private String name;

	private boolean concrete;

	private String sample;

	public boolean isMultiline() {
		return sample != null && sample.split("\n").length > 1;
	}

	@Override
	public String toString() {
		return ((type instanceof Class) ? ((Class<?>) type).getCanonicalName() : type.toString()) + " " + name;
	}

}
