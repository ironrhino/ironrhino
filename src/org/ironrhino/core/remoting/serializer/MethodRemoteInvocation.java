package org.ironrhino.core.remoting.serializer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.util.ClassUtils;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
class MethodRemoteInvocation extends RemoteInvocation {

	private static final long serialVersionUID = -2740913342844528055L;

	private transient Method method;

	MethodRemoteInvocation(MethodInvocation methodInvocation) {
		super(methodInvocation);
		this.method = methodInvocation.getMethod();
	}

	@Override
	public Object invoke(Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		if (method == null)
			method = ClassUtils.getInterfaceMethodIfPossible(
					targetObject.getClass().getMethod(getMethodName(), getParameterTypes()));
		return method.invoke(targetObject, getArguments());
	}

}