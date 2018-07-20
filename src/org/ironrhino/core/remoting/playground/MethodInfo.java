package org.ironrhino.core.remoting.playground;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import lombok.Data;

@Data
public class MethodInfo implements Serializable {

	private static final long serialVersionUID = 1311380262742909281L;

	private Method method;

	private String name;

	private Type returnType;

	private ParameterInfo[] parameters;

	public boolean isConcrete() {
		for (ParameterInfo p : parameters) {
			if (!p.isConcrete())
				return false;
		}
		return true;
	}

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
