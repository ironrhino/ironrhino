package org.ironrhino.core.remoting.playground;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import lombok.Data;

@Data
public class MethodInfo {

	private Method method;

	private String name;

	private Type returnType;

	private ParameterInfo[] parameters;

	public String getSignature() {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append('(');
		for (int i = 0; i < parameters.length; i++) {
			sb.append(parameters[i]);
			if (i < parameters.length - 1)
				sb.append(", ");
		}
		sb.append(") : ");
		if (returnType instanceof Class) {
			sb.append(((Class<?>) returnType).getCanonicalName());
		} else {
			sb.append(returnType.toString());
		}
		return sb.toString();
	}

}
