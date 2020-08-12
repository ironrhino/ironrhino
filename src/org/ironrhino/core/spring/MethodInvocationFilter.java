package org.ironrhino.core.spring;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.util.CheckedFunction;

@FunctionalInterface
public interface MethodInvocationFilter {

	Object filter(MethodInvocation methodInvocation,
			CheckedFunction<MethodInvocation, Object, Throwable> actualInvocation) throws Throwable;

}
