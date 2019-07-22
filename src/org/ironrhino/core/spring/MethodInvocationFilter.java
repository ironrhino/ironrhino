package org.ironrhino.core.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.util.ThrowableFunction;

public interface MethodInvocationFilter {

	Object filter(MethodInvocation methodInvocation,
			ThrowableFunction<MethodInvocation, Object, Throwable> actualInvocation) throws Throwable;

}
