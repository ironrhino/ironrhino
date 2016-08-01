package org.ironrhino.core.remoting;

import java.lang.reflect.Type;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;

public class GenericRemoteInvocation extends RemoteInvocation {

	private static final long serialVersionUID = -2740913342844528055L;

	private String[] genericParameterTypes;

	private String genericReturnType;

	public GenericRemoteInvocation() {

	}

	public GenericRemoteInvocation(MethodInvocation methodInvocation) {
		super(methodInvocation);
		Type[] types = methodInvocation.getMethod().getGenericParameterTypes();
		genericParameterTypes = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			Type type = types[i];
			genericParameterTypes[i] = JsonHttpInvokerSerializationHelper.toCanonical(type);
		}
		Type type = methodInvocation.getMethod().getGenericReturnType();
		genericReturnType = JsonHttpInvokerSerializationHelper.toCanonical(type);
	}

	public String[] getGenericParameterTypes() {
		return genericParameterTypes;
	}

	public void setGenericParameterTypes(String[] genericParameterTypes) {
		this.genericParameterTypes = genericParameterTypes;
	}

	public String getGenericReturnType() {
		return genericReturnType;
	}

	public void setGenericReturnType(String genericReturnType) {
		this.genericReturnType = genericReturnType;
	}

}
